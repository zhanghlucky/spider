package com.hui.zhang.spider.common.beanutil;

import java.io.Serializable;
import java.util.List;

/**
 * Created by zhanghui on 2017/11/30.
 */
public class JavaBeanParam implements Serializable {
    public JavaBeanDescriptor mainDescriptor;
    public List<JavaBeanDescriptor> typeDescriptorList;

    public JavaBeanParam (JavaBeanDescriptor mainDescriptor, List<JavaBeanDescriptor> typeDescriptorList){
        this.mainDescriptor=mainDescriptor;
        this.typeDescriptorList=typeDescriptorList;
    }
    public JavaBeanDescriptor getMainDescriptor() {
        return mainDescriptor;
    }

    public void setMainDescriptor(JavaBeanDescriptor mainDescriptor) {
        this.mainDescriptor = mainDescriptor;
    }

    public List<JavaBeanDescriptor> getTypeDescriptorList() {
        return typeDescriptorList;
    }

    public void setTypeDescriptorList(List<JavaBeanDescriptor> typeDescriptorList) {
        this.typeDescriptorList = typeDescriptorList;
    }
}
