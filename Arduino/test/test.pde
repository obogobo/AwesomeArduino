import processing.serial.*;

Serial port;

void setup() {
  for(String name : Serial.list())
    println(name);
  
  String portName = Serial.list()[5];
  port = new Serial(this, portName, 9600);
  delay(1000);
  port.write("@1234567890");
}

void draw() {
  delay(2000);
  port.write("@time:" + frameCount);
}


