package com.hui.zhang.spider.common.util;

import com.alibaba.fastjson.JSON;
import com.hui.zhang.spider.remoteing.netty.client.handler.RpcClientHandler;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by zhanghui on 2017/11/16.
 */
public class ClassMapUtil {
    private static final Logger logger = LoggerFactory.getLogger(ClassMapUtil.class);
    private final static  String DEFAULT_KEY="map_default_key";

    /*public static Map<String,Object>  toMap(String fieldName,Class cls){
        Map<String,Object> jsonMap=new HashedMap();
        build(fieldName,cls,jsonMap);
        return  jsonMap;
    }*/
    public static Object  getChildObj(String fieldName, Class cls, Type type){
        Map<String,Object> jsonMap=new HashedMap();
        build(fieldName,cls,type,jsonMap);
        return  jsonMap.get(fieldName);
    }

    /**
     * 递归映射对象类型
     * @param fieldName
     * @param cls
     * @param type
     * @param jsonMap
     */
    private  static void build(String fieldName, Class cls,Type type, Map<String,Object> jsonMap){
        if (cls.getName().equals("int")||
                cls.getName().equals(Integer.class.getName())||
                cls.getName().equals("long")||
                cls.getName().equals(Long.class.getName())
                ){
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,0);
            }else{
                jsonMap.put(fieldName,0);
            }

        }else if (cls.getName().equals("boolean")||
                cls.getName().equals(Boolean.class.getName())){

            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,true);
            }else{
                jsonMap.put(fieldName,true);
            }
        }else if (cls.getName().equals("double")||
                cls.getName().equals(Double.class.getName())){

            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,0.0);

            }else{
                jsonMap.put(fieldName,0.0);
            }
        }else if (cls.getName().equals("byte")||
                cls.getName().equals(Byte.class.getName())){
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,0);
            }else{
                jsonMap.put(fieldName,0);
            }
        }else if (cls.getName().equals(String.class.getName())){

            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,"");
            }else{
                jsonMap.put(fieldName,new String(""));
            }
        }else if(cls.getName().equals(List.class.getName())){//list类型
            List ls=new ArrayList();
            if (type instanceof ParameterizedType){//泛型的参数类型
                ParameterizedType pt = (ParameterizedType) type;
                Class  fieldClazz = (Class) pt.getActualTypeArguments()[0]; //得到泛型里的class类型对象。
                Map<String,Object> pMap=new HashedMap();
                build(null,fieldClazz,null,pMap);
                ls.add(pMap.get(DEFAULT_KEY));
            }
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,ls);
            }else{
                jsonMap.put(fieldName,ls);

            }
        }else if(cls.getName().equals(Map.class.getName())){
            Map map=new HashedMap();
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,map);
            }else{
                jsonMap.put(fieldName,map);
            }
        }else if(cls.getName().equals(BigDecimal.class.getName())){
            BigDecimal bigDecimal=new BigDecimal(0.0);
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,bigDecimal);
            }else {
                jsonMap.put(fieldName,bigDecimal);
            }

        }else if(cls.getName().equals(Date.class.getName())){
            Date date=new Date();
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,date);
            }else {
                jsonMap.put(fieldName,date);
            }
        }else if(cls.getName().contains("[")){//数组类型
            Object[] objArray=new Object[1];
            String clsName=cls.getName();
            String arrayType=clsName.substring(0,2);
            if (arrayType.equals("[I")){//int
                objArray=new Integer[1];
                objArray[0]=0;
            }else if (arrayType.equals("[J")){//long
                objArray=new Long[1];
                objArray[0]=0L;
            }else if (arrayType.equals("[D")){//double
                objArray=new Double[1];
                objArray[0]=0.0;
            }else if (arrayType.equals("[B")){//byte
                objArray=new Byte[1];
                objArray[0]=1;
            }else if (arrayType.equals("[Z")){//boolean
                objArray=new Boolean[1];
                objArray[0]=true;
            }else{//对象类型
                //System.out.println(clsName);
                //System.out.println(arrayType);
                String objType=clsName.substring(2,clsName.length()-1);
                try {
                    Class objCls=Class.forName(objType);
                    Map<String,Object> jMap=new HashedMap();
                    build(null,objCls,null,jMap);
                    objArray[0]=jMap.get(DEFAULT_KEY);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,objArray);
            }else {
                jsonMap.put(fieldName,objArray);
            }
        } else {//对象类型
            Map<String,Object> fMap=new HashedMap();
            Field[] fields= cls.getDeclaredFields();
            for (int i=0;i<fields.length;i++) {
                Field field=fields[i];
                Type ftype=field.getGenericType();
                build(field.getName(),field.getType(),ftype,fMap);
            }
            if (null==fieldName){
                jsonMap.put(DEFAULT_KEY,fMap);
            }else{
                jsonMap.put(fieldName,fMap);
            }
        }
    }

}
