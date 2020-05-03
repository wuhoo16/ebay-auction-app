/*
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */

class Message {
  String type;
  String input;
  int number;

  protected Message() {
    this.type = "";
    this.input = "";
    this.number = 0;
    System.out.println("client-side message created");
  }

  protected Message(String type, String input, int number) {
    this.type = type;
    this.input = input;
    this.number = number;
    System.out.println("client-side message created");
  }
}