package models;


public class WordModel {
    private String wordName;
    private int wordCount;


    public WordModel(String wordName, int wordCount) {
        this.wordName = wordName;
        this.wordCount = wordCount;
    }

    public String getWordName() {
        return wordName;
    }

    public void setWordName(String wordName) {
        this.wordName = wordName;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

}
