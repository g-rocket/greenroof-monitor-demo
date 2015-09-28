void setup() {
  Serial.begin(9600);
  pinMode(A0,INPUT);
}

void loop() {
  while(!Serial.available());
  Serial.read();
  int val = analogRead(A0);
  Serial.write((val >> 0) & 0x3f);
  Serial.write((val >> 6) & 0x3f);
}
