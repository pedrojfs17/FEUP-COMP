.class WhileAndIF
.super java/lang/Object

.method <init>()V
	.limit stack 1
	.limit locals 1

	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 4
	.limit locals 13

	bipush 20
	istore_1
	bipush 10
	istore_2
	bipush 10
	newarray int
	astore_3
	iload_1
	iload_2
	if_icmplt True1
	iconst_0
	goto Store1
True1:
	iconst_1
Store1:
	istore 5
	iload 5
	iload 5
	if_icmpne else0
	iload_1
	iconst_1
	isub
	istore 6
	goto endif0
else0:
	iload_2
	iconst_1
	isub
	istore 6
endif0:
	goto Condition1
Loop1:
	aload_3
	iload 6
	iload_1
	iload_2
	isub
	iastore
	iload 6
	iconst_1
	isub
	istore 6
	iload_1
	iconst_1
	isub
	istore_1
	iload_2
	iconst_1
	isub
	istore_2
Condition1:
	iconst_m1
	iload 6
	if_icmplt True2
	iconst_0
	goto Store2
True2:
	iconst_1
Store2:
	istore 7
	iload 7
	iload 7
	if_icmpeq Loop1
	iconst_0
	istore 6
	goto Condition2
Loop2:
	aload_3
	iload 6
	iaload
	istore 8
	iload 8
	invokestatic io.println(I)V
	iload 6
	iconst_1
	iadd
	istore 6
Condition2:
	aload_3
	arraylength
	istore 10
	iload 6
	iload 10
	if_icmplt True3
	iconst_0
	goto Store3
True3:
	iconst_1
Store3:
	istore 11
	iload 11
	iload 11
	if_icmpeq Loop2
	return
.end method
