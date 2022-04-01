# Mongodb-Schema-Autogen-4-Java

This is an IDEA plugin project that can be installed from plugins Marketplace.

## Usage scenario  
	When you create a java entity file and want to convert it into a js script that can generate Mongo Schema

## Usage
```
 1. Make sure project can compile successfully
 2. Right-click on the Java file
 3. Find "Generate Mongo Schema" below "Find Usages"
 4. The "to clipboard" on the right generates script
 5. Ctrl+v
```

## example
```
public class Entity{
	private ObjectId id;
	private Integer num;
}

will be converted to:  

db.entity.runCommand({
    collMod: "entity",

    validator: {
        "$jsonSchema": {
            "additionalProperties": false,
            "properties": {
                "_id": {
                    "bsonType": "objectId",
                    "title": ""
                },
                "num": {
                    "bsonType": "int",
                    "title": ""
                }
            }
        }
    }})
```
