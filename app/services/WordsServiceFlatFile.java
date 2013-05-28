package services;


import akka.actor.*;
import models.WordModel;
import org.springframework.stereotype.Service;
import play.Configuration;
import play.Play;
import play.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WordsServiceFlatFile implements WordsService {

    private static Configuration config = Play.application().configuration();
    private String fullWordsPath = config.getString("words.file.path") + "/" +config.getString("words.file.name");

    private volatile List<WordModel> wordModelList;
    private ActorSystem actorSystem = ActorSystem.create("WordsActorSystem");
    private ActorRef writeWordsFileActor;

    public WordsServiceFlatFile() {
        wordModelList = loadWords();
        writeWordsFileActor = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new WriteWordsActor();
            }
        }),
        "wordsWriteActor");
    }

    @Override
    public WordModel insertWord(String wordName) {
        WordModel word = new WordModel(wordName, 0);
        return insertWord(word);
    }

    @Override
    public boolean updateWord(String wordName, int count) {
        WordModel word = new WordModel(wordName, count);
        return updateWordCount(word);
    }

    @Override
    public List<WordModel> listWords() {
        return wordModelList;
    }

    @Override
    public WordModel getWordCount(String wordName) {
        return findWord(wordName);
    }

    @Override
    public WordModel getWordRank(String wordName) {
        return findWord(wordName);
    }

    private List<WordModel> loadWords() {
        BufferedReader reader = getWordReader();
        List<WordModel> wordsList = new ArrayList<WordModel>();
        String currentWord = "";
        try {
            while ((currentWord = reader.readLine()) != null) {
                String[] w = currentWord.split(",");
                String wordName = w[0].toLowerCase();
                wordsList.add(new WordModel(wordName, Integer.parseInt(w[1])));
            }
        }
        catch(IOException e) {
            Logger.error("IOException trying to read file", e);
        }
        finally {
            try {
                reader.close();
            }
            catch(IOException e){
                Logger.error("Exception closing words.txt buffered reader", e);
            }
        }
        return new CopyOnWriteArrayList<WordModel>(wordsList); // Thread Safe but slow for our big list!
    }

    /**
     * Manual Binary Search. Could just use Collections with compare.
     *
     */
    private WordModel findWord(String wordName) {
        WordModel word = new WordModel(wordName,0);
        List<WordModel> findList = wordModelList;
        while(true) {
            WordModel model = null;
            if(findList.size() == 1) {
                model = findList.get(0);
                if(!model.getWordName().equals(wordName)) {
                    break;
                }
            }
            else {
                int middle = findList.size() >>> 1;
                model = findList.get(middle);
                model.setRank(middle + 1);
            }

            if(model.getWordName().equals(wordName)) {
                return model;
            }
            int middle = findList.size() >>> 1;
            if(wordName.compareToIgnoreCase(model.getWordName()) < 0) {
                findList = findList.subList(0, middle);
            }
            else {
                findList = findList.subList(middle, findList.size());
            }
        }
        findList = null;
        return word;
    }

    private boolean updateWordCount(WordModel newWordCount) {
        WordModel word = findWord(newWordCount.getWordName());
        if(word.getWordCount() == 0) {
            insertWord(word);
        }
        else {
            word.setWordCount(newWordCount.getWordCount());
            writeWordsFileActor.tell("write words file");
        }
        return true;
    }

    private WordModel insertWord(WordModel word) {
        word = findWord(word.getWordName());
        if(word.getWordCount() == 0) {
            word.setWordCount(1);
            wordModelList.add(word);
            sortWordList();
        }
        else {
            word.setWordCount(word.getWordCount() + 1);
        }

        writeWordsFileActor.tell("write words file");
        System.out.println("AFTER WRITE WORDS ACTOR CALL!");

        return word;
    }

    private boolean writeWordsFile() {
        File tempWordFile = renameWordsFile();
        BufferedWriter writer = null;
        FileWriter fWriter = null;
        try {
            fWriter = new FileWriter(getWordsFile());
            writer = new BufferedWriter(fWriter);
            for(WordModel word : wordModelList) {
                writer.write(word.getWordName() + "," + word.getWordCount());
                writer.newLine();
                writer.flush();
            }
        }
        catch(Exception e) {
            Logger.error("IOException writing file", e);
            if(deleteFile(getWordsFile())) {
                reinstateTempWordsFile(tempWordFile);
            }
        }
        finally {
            try {
                fWriter.close();
                writer.close();
            }
            catch(IOException e){
                Logger.error("Exception writing to words file", e);
            }
        }
        deleteFile(tempWordFile);
        return true;
    }

    private BufferedReader getWordReader() {
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(getWordsFile()));
        }
        catch(IOException e) {
            Logger.error("IOException trying load writer", e);
        }
        return bReader;
    }

    private File getWordsFile() {
        File wordFile = new File(fullWordsPath);
        try {
            if(!wordFile.exists()) {
                wordFile.createNewFile();
            }
        }
        catch(IOException e) {
            Logger.error("IOException trying to load file: " + fullWordsPath, e);
        }
        return wordFile;
    }

    private File renameWordsFile() {
        File wordFile = getWordsFile();
        File tempWordsFile = new File(config.getString("words.file.path") + "/wordfile.temp");
        if(wordFile.exists()) {
            boolean result = wordFile.renameTo(tempWordsFile);
            if(result) {
                wordFile.delete();
            }
            Logger.debug("Temp File Path: " + wordFile.getAbsolutePath());
        }
        return tempWordsFile;
    }

    private boolean reinstateTempWordsFile(File tempWordsFile) {
        boolean result = tempWordsFile.renameTo(new File(fullWordsPath));
        Logger.debug("Temp File Reinstated: " + tempWordsFile.getAbsolutePath());
        return result;
    }

    private boolean deleteFile(File fileToDelete) {
        return fileToDelete.delete();
    }

    private void sortWordList() {
        Collections.sort(wordModelList, new Comparator<WordModel>() {
            @Override
            public int compare(WordModel wordModel, WordModel wordModel2) {
                return wordModel.getWordName().compareTo(wordModel2.getWordName());
            }
        });
    }


    /**
     * Experimenting with akka...
     */
    private class WriteWordsActor extends UntypedActor {

        private BlockingQueue<Date> writeQueue = new ArrayBlockingQueue<Date>(2);

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {

                Logger.info("Received String message: " + message);

                if(((String) message).contains("write words file")) {

                    Logger.debug("QUEUE REMAINNG CAPACITY: " + writeQueue.remainingCapacity());

                    if(writeQueue.remainingCapacity() == 0) {
                        Logger.debug("Reached Write Words Capacity. No need to add. Next write will pick up changes.");
                        return;
                    }

                    writeQueue.add(new Date());

                    doQueue();
                }

            }
            else {
                this.getContext().stop(this.getSelf());
                unhandled(message);
            }
        }

        private void doQueue() {
            for(int i = 0; i < 3; i++) {
                Logger.debug("Write Words File!");

                long startTime = System.currentTimeMillis();

                boolean result = writeWordsFile();

                long stopTime = System.currentTimeMillis();

                long elapsedTime = (stopTime - startTime) / 1000;
                Logger.debug("File Write Took " + elapsedTime + " Seconds.");

                if(result) {
                    try {
                        writeQueue.take();
                    }
                    catch (InterruptedException e) {
                        Logger.error("Trying to remove write job from queue", e);
                    }
                    break;
                }
            }
        }

        @Override
        public void preStart() {
            super.preStart();
            Logger.debug("Write Words File Actor STARTED");
        }

        @Override
        public void postStop() {
            super.postStop();
            Logger.debug("Write Words File Actor STOPPED");
        }
    }
}



