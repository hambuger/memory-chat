import com.github.hambuger.memory.chat.wechat.service.LoginService;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@SpringBootApplication(scanBasePackages = "com.github.hambuger", exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class Application {

    private static ConfigurableApplicationContext context;


    public static void main(String[] args) {
        context = new SpringApplicationBuilder(Application.class).headless(false).run();
        LoginService loginService = context.getBean(LoginService.class);
        loginService.login();

    }

}
