package finance.defi.config;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_.@A-Za-z0-9-]*$";
    public static final String PHONE_REGEX = "^\\+(?:[0-9] ?){6,14}[0-9]$";
    
    public static final String SYSTEM_ACCOUNT = "system";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String ANONYMOUS_USER = "anonymoususer";

    public static final String ETH = "ETH";
    public static final String DAI = "DAI";
    public static final String USDC = "USDC";
    public static final String BAT = "BAT";

    private Constants() {
    }
}
