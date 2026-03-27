package com.smart.retry.test;

/**
 * @Author xiaoqiang
 * @Version TestModel.java, v 0.1 2025年02月21日 12:25 xiaoqiang
 * @Description: TODO
 */
public class TestModel {
    private String id;

    private String name;

    private int age;

    private ModelBuilder modelBuilder;



    public TestModel(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public ModelBuilder getModelBuilder() {
        return modelBuilder;
    }

    public void setModelBuilder(ModelBuilder modelBuilder) {
        this.modelBuilder = modelBuilder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }


    public static class ModelBuilder {

        private String id;
        private String name;
        private int age;

        public ModelBuilder id(String id) {
            this.id = id;
            return this;
        }
        public ModelBuilder name(String name) {
            this.name = name;
            return this;
        }
        public ModelBuilder age(int age) {
            this.age = age;
            return this;
        }

    }
}

