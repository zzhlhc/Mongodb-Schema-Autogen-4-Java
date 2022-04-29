import java.util.List;

public class EntityForTest {
    Boolean boolean1;
    Byte byte1;
    Character character1;
    Short short1;
    Integer integer1;
    Float float1;
    Double double1;
    List<Double> doubles;
    List<Integer> integers;

    public static void main(String[] args) {
        System.out.println(Generator.generate(EntityForTest.class));
    }
}

