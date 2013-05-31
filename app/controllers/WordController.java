package controllers;

import models.WordModel;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import play.Logger;
import play.api.libs.ws.WS;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import services.WordsService;

import java.util.List;
import java.util.concurrent.Callable;

import static play.mvc.Controller.ok;
import static play.mvc.Results.async;

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
        Logger.debug("Word Count Returned!");
        return ok(json);
    }

    public Result getWordRank(String wordName) {
        WordModel word = wordsService.getWordRank(wordName);
        ObjectNode json = Json.newObject();
        json.put("Ranking", word.getRank());
        return ok(json);
    }

    public Result listWords() {
        //Logger.debug("Before Word List Call!");
        F.Promise<List<WordModel>> wordListPromise = play.libs.Akka.future(
                new Callable<List<WordModel>>() {
                    public List<WordModel> call() {
                        return wordsService.listWords();
                    }
                }
        );
        return async(
                wordListPromise.map(
                        new F.Function<List<WordModel>, Result>() {
                            public Result apply(List<WordModel> wordList) {
                                /**
                                try {
                                    Thread.sleep(10000);  // Lets test the async.
                                }
                                catch (InterruptedException e) {}
                                Logger.debug("Word List Returned!");
                                 **/
                                return ok(Json.toJson(wordList).toString());
                            }
                        }
                )
        );
    }

}
