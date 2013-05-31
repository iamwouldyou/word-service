import configs.AppConfig;
import models.WordModel;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import services.WordsServiceFlatFile;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;


@ContextConfiguration(classes={AppConfig.class})
public class WordServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private WordsServiceFlatFile wordsServiceFlatFile;

    @Test
    public void testGetWordRank() {
        WordModel word = wordsServiceFlatFile.getWordRank("bridge");
        System.out.println("WORD: " + word.getWordName());
        assertThat(word.getWordName().equals("bridge"));
    }
}
