public enum Colors {
    RED("\u001B[31m")
    , GREEN("\u001B[32m")
    , RESET("\u001B[0m")
    , BLUE("\u001B[34m")
    , YELLOW("\u001B[33m")
    , PURPLE("\u001B[35m")
    , CYAN("\u001B[36m")
    , ORANGE("\u001B[38;5;208m")
    , WHITE("\u001B[37m")
    , BLACK("\u001B[30m");

    private String code;
    Colors(String code) {
        this.code  = code;
    }

    public String getCode(){
        return code;
    }
}
