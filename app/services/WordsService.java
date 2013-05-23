package services;

import models.WordModel;

import java.util.ArrayList;
import java.util.List;

public interface WordsService {

    public boolean updateWord(String word, int count);

    public WordModel insertWord(String word);

    public List<WordModel> listWords();

    public List<WordModel> listWordsByRanking();

    public WordModel getWordCount(String word);

    public WordModel getWordRank(String word);
}
