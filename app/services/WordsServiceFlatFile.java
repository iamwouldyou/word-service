package services;


import models.WordModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.Configuration;
import play.Play;
import scalax.io.support.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The number of words in a dictionary can vary based on publisher.. up to 500k.
 * As our file gets larger we may need to restrict list of words to a letter or range of letters.
 * or
 * Use asynchronous functionality to load the words in the background.
 */
@Service
public class WordsServiceFlatFile implements WordsService {
    static final Logger log = LoggerFactory.getLogger(WordsServiceFlatFile.class);

    private static Configuration config = Play.application().configuration();
    private String fullWordsPath = config.getString("words.file.path") + "/" +config.getString("words.file.name");
    private volatile List<WordModel> wordModelList;

    public WordsServiceFlatFile() {
        wordModelList = loadWords();
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
        sortWordList();
        return wordModelList;
    }

    @Override
    public List<WordModel> listWordsByRanking() {
        return getAllWordsByRank();
    }

    @Override
    public WordModel getWordCount(String wordName) {
        return findWord(wordName);
    }


    private List<WordModel> loadWords() {
        BufferedReader reader = getWordReader();
        ArrayList<WordModel> wordsList = new ArrayList<WordModel>(236000);
        String currentWord = "";
        try {
            while ((currentWord = reader.readLine()) != null) {
                String[] w = currentWord.split(",");
                String wordName = w[0].toLowerCase();
                wordsList.add(new WordModel(wordName, Integer.parseInt(w[1])));
            }
        }
        catch(IOException e) {
            log.error("IOException trying to read file", e);
        }
        finally {
            try {
                reader.close();
            }
            catch(IOException e){
                log.error("Exception closing words.txt buffered reader", e);
            }
        }
        return wordsList;
    }

    private List<WordModel> getAllWordsByRank() {
        List<WordModel> sortedByRankWords = wordModelList;
        Collections.sort(sortedByRankWords, new Comparator<WordModel>() {
            @Override
            public int compare(WordModel wordModel, WordModel wordModel2) {
                if(wordModel.getWordCount() == wordModel2.getWordCount()) {
                    return 0;
                }
                return wordModel.getWordCount() < wordModel2.getWordCount() ? 1 : -1;
            }
        });
        return sortedByRankWords;
    }

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
            word = insertWord(word);
        }
        else {
            word.setWordCount(newWordCount.getWordCount());
            writeWordsFile();
        }
        return true;
    }

    private WordModel insertWord(WordModel word) {
        word = findWord(word.getWordName());
        if(word.getWordCount() == 0) {
            word.setWordCount(1);
            wordModelList.add(word);
        }
        else {
            word.setWordCount(word.getWordCount() + 1);
        }
        writeWordsFile();
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
            log.error("IOException writing file", e);
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
                log.error("Exception writing to words file", e);
            }
        }
        deleteFile(tempWordFile);
        return true;
    }

    public BufferedReader getWordReader() {
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(getWordsFile()));
        }
        catch(IOException e) {
            log.error("IOException trying load writer", e);
        }
        return bReader;
    }

    public File getWordsFile() {
        File wordFile = new File(fullWordsPath);
        try {
            if(!wordFile.exists()) {
                wordFile.createNewFile();
            }
        }
        catch(IOException e) {
            log.error("IOException trying to load file: " + fullWordsPath, e);
        }
        return wordFile;
    }

    public File renameWordsFile() {
        File wordFile = getWordsFile();
        File tempWordsFile = new File(config.getString("words.file.path") + "/wordfile.temp");
        if(wordFile.exists()) {
            boolean result = wordFile.renameTo(tempWordsFile);
            if(result) {
                wordFile.delete();
            }
            log.debug("Temp File Path: " + wordFile.getAbsolutePath());
        }
        return tempWordsFile;
    }

    public boolean reinstateTempWordsFile(File tempWordsFile) {
        boolean result = tempWordsFile.renameTo(new File(fullWordsPath));
        log.debug("Temp File Reinstated: " + tempWordsFile.getAbsolutePath());
        return result;
    }

    public boolean deleteFile(File fileToDelete) {
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
}
