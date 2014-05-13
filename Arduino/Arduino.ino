#include <LiquidCrystal.h>
#include <FrequencyTimer2.h>

LiquidCrystal lcd(12, 11, 9, 8, 7, 6, 5, 4, 3, 2);


/* 
 * 8x8 LED Matrix code
 * Source: http://playground.arduino.cc/Main/DirectDriveLEDMatrix
 */
// pin[xx] on led matrix connected to nn on Arduino (-1 is dummy to make array start at pos 1)
int pins[17]= {-1, 36, 35, 32, 31, 28, 27, 24, 23, 39, 40, 43, 44, 47, 48, 51, 52};
//5, 4, 3, 2, 14, 15, 16, 17, 13, 12, 11, 10, 9, 8, 7, 6};

// col[xx] of leds = pin yy on led matrix
int cols[8] = {pins[13], pins[3], pins[4], pins[10], pins[06], pins[11], pins[15], pins[16]};

// row[xx] of leds = pin yy on led matrix
int rows[8] = {pins[9], pins[14], pins[8], pins[12], pins[1], pins[7], pins[2], pins[5]};

byte col = 0;
byte leds[8][8];

void clearLeds() {
  for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 8; j++) {
      leds[i][j] = 0;
    }
  }
}

void setPattern(String pattern) {
  int i = 0;
  while(pattern[i] != '\0' && i < 64) {
    leds[i / 8][i % 8] = pattern[i] - '0';
    i++;
  }
  
  /*
  for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 8; j++) {
      Serial.print(leds[i][j]);
      Serial.print(" ");
    }
    Serial.print("\n");
  }
  */
}

void display() {
  digitalWrite(cols[col], HIGH);  // Turn whole previous column off
  col++;
  if (col == 8) {
    col = 0;
  }
  for (int row = 0; row < 8; row++) {
    if (leds[col][7 - row] == 1) {
      digitalWrite(rows[row], HIGH);  // Turn on this led
    }
    else {
      digitalWrite(rows[row], LOW); // Turn off this led
    }
  }
  digitalWrite(cols[col], LOW); // Turn whole column on at once (for equal lighting times)
}

void setup() {
  lcd.begin(16, 2);
  Serial.begin(9600);
  
  // sets the pins as output
  for (int i = 1; i <= 16; i++) {
    pinMode(pins[i], OUTPUT);
  }

  // set up cols and rows
  for (int i = 1; i <= 8; i++) {
    digitalWrite(cols[i - 1], LOW);
  }

  for (int i = 1; i <= 8; i++) {
    digitalWrite(rows[i - 1], LOW);
  }
  
  clearLeds();
  FrequencyTimer2::disable();
  FrequencyTimer2::setPeriod(2000);
  FrequencyTimer2::setOnOverflow(display);
  
  setPattern("1010101001010101101010100101010110101010010101011010101001010101");
}

void print_tweet(String input) {
  String uname = "";
  int i = 0;
  while(input[i] != '\n' && input[i] != '\0') {
    uname = uname + input[i];
    i += 1;
  }
  
  String date = "";
  if(input[i] == '\n' && input[i+1] != '\0') {
    i += 1;
    while(input[i] != '\0') {
      date = date + input[i]; 
      i += 1;
    }
  }
  
  Serial.println("uname: "+ uname);
  Serial.println("date: "+ date);
  
  lcd.clear();
  lcd.print(uname);
  lcd.setCursor(0, 1);
  lcd.print(date);
}

void loop() {
  if(Serial.available() > 0) {
    char buffer[65];
    memset(buffer, 0, 65);
    delay(50);
    Serial.readBytes(buffer, 64);
    
    for(int i = 0; i < 65; i++) {
      if(buffer[i] == '\n' || buffer[i] == '\0') continue;
      if(buffer[i] < 32) buffer[i] = ' '; 
    }
    
    String input(buffer);
    
    switch(input[0]) {
      case '@':
        print_tweet(input);
        Serial.println("**");
        break;
      case '0':
      case '1':
        setPattern(input);
        Serial.println("**");
        break;
      case 'C':
        lcd.clear();
        Serial.println("**");
        break;
      default:
        Serial.println("**");
    }
  }
}
