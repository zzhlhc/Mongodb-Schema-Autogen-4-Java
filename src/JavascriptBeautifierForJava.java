
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStreamReader;

public class JavascriptBeautifierForJava {

    private static final String BEAUTIFY_JS_RESOURCE ="beautify.js";

    // name of beautifier function
    private static final String BEAUTIFY_METHOD_NAME ="js_beautify";

    private final ScriptEngine engine;

    JavascriptBeautifierForJava() throws ScriptException {

        engine = new ScriptEngineManager().getEngineByName("nashorn");

        // this is needed to make self invoking function modules work
        // otherwise you won't be able to invoke your function
        engine.eval("var global = this;");
        engine.eval(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(BEAUTIFY_JS_RESOURCE)));
    }

    public String beautify(String javascriptCode) throws ScriptException, NoSuchMethodException {
        return (String) ((Invocable) engine).invokeFunction(BEAUTIFY_METHOD_NAME, javascriptCode);
    }

    public static void main(String[] args) throws ScriptException, NoSuchMethodException {

        JavascriptBeautifierForJava javascriptBeautifierForJava = new JavascriptBeautifierForJava();
        String formattedJs = javascriptBeautifierForJava.beautify("db.action.runCommand({\n" +
                " collMod: \"action\",\n" +
                "\n" +
                " validator:{\n" +
                "\"$jsonSchema\":{\n" +
                "\"additionalProperties\":false,\n" +
                "\"properties\":{\n" +
                "\"a\":{\n" +
                "\"bsonType\":\"int\",\n" +
                "\"title\":\"\"\n" +
                "},\n" +
                "\"b\":{\n" +
                "\"bsonType\":\"int\",\n" +
                "\"title\":\"\"\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "})");

        System.out.println(formattedJs);
        // will print out:
        //        var a = 1;
        //        b = 2;
        //        var user = {
        //            name:"Andrew"
        //        }
    }
}