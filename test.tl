local int x;

print("test while...");
x = 0;
while(x < 10){
	print("x->", x);
	x = x + 1;
}
print("test done...");

print("----");


print("test do while...");
x = 0;
do{
	print("x->", x);
	x = x + 1;
}while(x < 10);
print("test done...");

print("----");


print("test while again...");
x = 10;
while(x < 10){
	print("x->", x);
	x = x + 1;
}
print("test done...");

print("----");


print("test do while again...");
x = 10;
do{
	print("x->", x);
	x = x + 1;
}while(x < 10);
print("test done...");
