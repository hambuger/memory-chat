import com.github.hambuger.memory.chat.wechat.service.LoginService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@SpringBootApplication(scanBasePackages = "com.github.hambuger", exclude = DataSourceAutoConfiguration.class)
public class Application {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
        System.out.println("start 1");
        context = new SpringApplicationBuilder(Application.class).headless(false).run();
        System.out.println("start 2");
        LoginService loginService = context.getBean(LoginService.class);
        loginService.login();

    }

}
