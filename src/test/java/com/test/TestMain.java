package com.test;


import java.util.concurrent.TimeUnit;

/**
 * Created by zhanghui on 2017/11/16.
 */
public class TestMain {

    public  static  void  main(String argus[]){


        long l = TimeUnit.MILLISECONDS.toNanos(3);
        System.out.println(l);


        //Map json= ClassMapUtil.toMap("testField",RpcParam.class);
        //System.out.println(json);
     /*   List<RpcParam> list=new ArrayList<>();
*/
        /*ParameterizedType pt = (ParameterizedType)list.getClass().getGenericSuperclass();
        String typeName = pt.getActualTypeArguments()[0].getClass().getName();
        System.out.println(typeName);*/

       /* Method method = null;
        try {
            method = list.getClass().getMethod("get",int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Class returnTypeClass = method.getReturnType();
        System.out.println(returnTypeClass.getName());*/

       /* ParameterizedType parameterizedType = (ParameterizedType) list.getClass().getGenericSuperclass();//获取当前new对象的泛型的父类类型

        int index = 0;//第n个泛型    Map<K,V> 就有2个  拿K  就是0  V就是1


        Class clazz = (Class) parameterizedType.getActualTypeArguments()[index];
        System.out.println("clazz ==>> "+clazz);*/
   /*    System.out.println(int[].class.getName());
       System.out.println(int.class.getName());

        System.out.println(Integer[].class.getName());
        System.out.println(Integer.class.getName());


       System.out.println(long[].class.getName());
       System.out.println(long.class.getName());*/

      /*  System.out.println(Pager[].class.getName());
        System.out.println(Pager.class.getName());
*/
       /* System.out.println(int[].class.getName());
        System.out.println(long[].class.getName());
        System.out.println(double[].class.getName());
        System.out.println(BigDecimal[].class.getName());
        System.out.println(Date[].class.getName());
        System.out.println(String[].class.getName());
        System.out.println(byte[].class.getName());
        System.out.println(boolean[].class.getName());*/

       /* try {
            System.out.println(Class.forName("J"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
*/
//        RpcResponse rpcResponse=new RpcResponse();
//        JavaBeanDescriptor javaBeanDescriptor=new JavaBeanDescriptor(rpcResponse.getClass().getName(),1);
//       System.out.println(JsonEncoder.DEFAULT.encode(javaBeanDescriptor));



    }


}


