import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.EnumUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Generator {

    //获取所有需要转换的java类型(即在Bsontype中有别名)
    private static final List<String> allJavaFieldTypesNeedConvert = Arrays.stream(JavaFieldType.values()).
            map(JavaFieldType::name).
            collect(Collectors.toList());

    public static String generate(Class<?> t) {
        //构造$jsonSchema基础结构
        JSONObject validator = new JSONObject(true);
        JSONObject properties = new JSONObject(true);
        validator.put("$jsonSchema", properties);
        JSONObject propertiesV = new JSONObject(true);
        //开始转换
        properties.put("additionalProperties", false);
        properties.put("properties", recurseToTraverseFields(t, propertiesV));
        //输出结果
        StringBuilder rs = new StringBuilder();
        String collName = t.getSimpleName();
        collName = collName.substring(0, 1).toLowerCase().concat(collName.substring(1));
        rs.append("db.").append(collName)
                .append(".runCommand({ \n collMod: \"")
                .append(collName)
                .append("\", \n validator:")
                .append(JSONObject.toJSONString(validator, SerializerFeature.SortField))
                .append("})");
        return prettyFormat(rs.toString());
    }

    private static String prettyFormat(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.toCharArray().length; i++) {
            sb.append(s.charAt(i));
            if (s.charAt(i) == ',') {
                sb.append("\n");
            } else if (i > 0 && (s.startsWith("{\"", i)
                    || s.startsWith("\"}", i)
                    || s.startsWith("}}", i)
                    || s.startsWith("[\"", i)
                    || s.startsWith("]}", i)
                    || s.startsWith("\"]", i))) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    private static JSONObject recurseToTraverseFields(Class<?> t, JSONObject propertiesV) {
        //收集需要处理的字段
        List<Field> fieldsToLoop = Arrays.stream(t.getDeclaredFields())
                .filter(f -> !t.getSimpleName().equals(f.getType().getSimpleName()))
                .collect(Collectors.toList());
        Class<?> superclass = t.getSuperclass();

        if (superclass != null && superclass.getName().equals("coopwire.common.base.mongo.BaseEntity")) {
            fieldsToLoop.addAll(Arrays.stream(superclass.getDeclaredFields()).collect(Collectors.toList()));
        }

        //开始处理
        for (Field curField : fieldsToLoop) {
            Class<?> curFieldClass = curField.getType();
            String curFieldClassName = curFieldClass.getSimpleName();
            JSONObject v = new JSONObject(true);
            propertiesV.put(curField.getName().equals("id") ? "_id" : curField.getName(), v);
            if (!isCollection(curFieldClass)) {
                //当前field非数组
                if (needNotRecurse(curFieldClass)) {
                    //非内部类无需递归
                    if (EnumUtils.isValidEnum(JavaFieldType.class, curFieldClassName)) {
                        v.put("bsonType", JavaFieldType.valueOf(curFieldClassName).getMongoFieldType());
                    } else if (isEnum(curFieldClass)) {
                        v.put("bsonType", "string");
                        v.put("enum", Arrays.stream(curFieldClass.getDeclaredFields()).
                                filter(x -> !x.getName().equals("$VALUES")).
                                map(Field::getName).
                                collect(Collectors.toList()));
                    } else {
                        v.put("bsonType", curFieldClassName);
                    }
                } else {
                    //非包装类型的内部类需递归
                    v.put("bsonType", "object");
                    v.put("properties", recurseToTraverseFields(curFieldClass, new JSONObject(true)));
                }
            } else {
                //当前field是数组
                v.put("bsonType", "array");
                JSONObject itemsV = new JSONObject(true);
                v.put("items", itemsV);
                Class<?> nextT = (Class<?>) getGenericClass(curField);
                if (needNotRecurse(nextT)) {
                    //无需递归
                    if (EnumUtils.isValidEnum(JavaFieldType.class, nextT.getSimpleName())) {
                        itemsV.put("bsonType", JavaFieldType.valueOf(nextT.getSimpleName()).getMongoFieldType());
                    } else if (Enum.class.isAssignableFrom(nextT)) {
                        itemsV.put("bsonType", "string");
                        itemsV.put("enum", Arrays.stream(nextT.getDeclaredFields()).
                                filter(x -> !x.getName().equals("$VALUES")).
                                map(Field::getName).
                                collect(Collectors.toList()));
                    }
                } else {
                    //需递归
                    itemsV.put("bsonType", "object");
                    itemsV.put("properties", recurseToTraverseFields(nextT, new JSONObject(true)));
                }
            }
            v.put("title", "");
        }
        return propertiesV;
    }

    private static boolean isBoxForPrimitive(Class<?> curFieldClass) {
        try {
            return ((Class) curFieldClass.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }

    }

    private static boolean isEnum(Class<?> curFieldClass) {
        return Enum.class.isAssignableFrom(curFieldClass);
    }

    private static boolean isCollection(Class<?> curFieldClass) {
        return Collection.class.isAssignableFrom(curFieldClass);
    }

    private static Type getGenericClass(Field declaredField) {
        return ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments()[0];
    }

    private static boolean needNotRecurse(Class<?> declaredFieldClass) {
        return declaredFieldClass.isPrimitive()
                || EnumUtils.isValidEnum(JavaFieldType.class, declaredFieldClass.getSimpleName())
                || Enum.class.isAssignableFrom(declaredFieldClass)
                || isBoxForPrimitive(declaredFieldClass);
    }

    private enum JavaFieldType {
        Double("double"),
        String("string"),
        Object("object"),
        JSONObject("object"),
        Array("array"),
        ObjectId("objectId"),
        Boolean("bool"),
        Date("date"),
        Null("null"),
        Integer("int"),
        BigDecimal("string"),
        Long("long"),
        Short("int"),
        Byte("int"),
        ;

        private final String mongoFieldType;

        JavaFieldType(String mongoFieldType) {
            this.mongoFieldType = mongoFieldType;
        }

        private String getMongoFieldType() {
            return mongoFieldType;
        }

    }

}
