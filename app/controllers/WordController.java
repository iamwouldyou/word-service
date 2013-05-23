package controllers;

import models.WordModel;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.Json;
import play.mvc.Result;
import services.WordsService;

import static play.mvc.Controller.ok;

@org.springframework.stereotype.Controller
public class WordController {

    @Autowired
    private WordsService wordsService;

    public Result updateWord(String word, int count) {
        boolean success = wordsService.updateWord(word, count);
        ObjectNode node = Json.newObject();
        node.put("success", true);
        if(!success) {
            node.put("success", false);
        }
        return ok(node);
    }

    public Result insertWord(String word) {
        String json = Json.toJson(wordsService.insertWord(word)).toString();
        return ok(json);
    }

    public Result getWordCount(String word) {
        String json = Json.toJson(wordsService.getWordCount(word)).toString();
        return ok(json);
    }

    public Result getWordRank(String wordName) {
        WordModel word = wordsService.getWordRank(wordName);
        ObjectNode json = Json.newObject();
        json.put("Ranking", word.getRank());
        return ok(json);
    }

    public Result listWords() {
        String json = Json.toJson(wordsService.listWords()).toString();
        return ok(json);
    }

    public Result listWordsRanked() {
        String json = Json.toJson(wordsService.listWordsByRanking()).toString();
        return ok(json);
    }

}
