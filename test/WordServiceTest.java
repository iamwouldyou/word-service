import configs.AppConfig;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import services.WordsServiceFlatFile;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

@ContextConfiguration(classes={AppConfig.class})
public class WordServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private WordsServiceFlatFile wordsServiceFlatFile;


}
