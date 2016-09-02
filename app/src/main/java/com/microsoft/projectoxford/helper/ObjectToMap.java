package com.microsoft.projectoxford.helper;

import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.contract.Category;
import com.microsoft.projectoxford.vision.contract.Face;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Scrok Brunoro
 * @create 2/9/16 19:50
 * @project cognitive-vision
 */
public class ObjectToMap {

    public static Map<String, Object> caption(Caption caption) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("confidence", caption.confidence);
        result.put("text", caption.text);
        return result;
    }
    public static Map<String, Object> category(Category category) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", category.name);
        result.put("score", category.score);
        return result;
    }
    public static Map<String, Object> face(Face face) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("age", face.age);
        result.put("gender", face.gender.toString());
        result.put("genderScore", Float.toString(face.genderScore));
        return result;
    }
}
