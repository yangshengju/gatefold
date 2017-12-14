package cn.gooday;

/**
 * A Camel Application
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception
    {
        String downLoadToUrl = args[0];
        ToolService toolService = new ToolServiceImpl();
        toolService.downLoadGateFold(downLoadToUrl);
        System.exit(0);
    }
}

