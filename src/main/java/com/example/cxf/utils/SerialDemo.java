package com.example.cxf.utils;

import java.io.*;

public class SerialDemo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("Hello, Serial Demo!");
        Save save = new Save(10, "Test");

        File f = new File("obj.txt");
        System.out.println(f.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(f);
        ObjectOutput oos = new ObjectOutputStream(fos);
        oos.writeObject(save);
        oos.close();


        FileInputStream fis = new FileInputStream(f);
        ObjectInput obji = new ObjectInputStream(fis);
        Save save1 = (Save) obji.readObject();

        System.out.println("a=" + save1.getA());

    }

}


class Save implements Serializable {
    int a;
    String b;

    public Save(int a, String b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}