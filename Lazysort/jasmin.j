.class Lazysort
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
	.limit locals 11

	bipush 10
	newarray int
	astore_1
	iconst_0
	istore_3
Loop0:
	aload_1
	arraylength
	istore 4
	iload_3
	iload 4
	if_icmplt True1
	iconst_0
	goto Store1
True1:
	iconst_1
Store1:
	istore 5
	iload 5
	iload 5
	if_icmpeq Body0
	goto EndLoop0
Body0:
	aload_1
	arraylength
	istore 6
	aload_1
	iload_3
	iload 6
	iload_3
	isub
	iastore
	iload_3
	iconst_1
	iadd
	istore_3
	goto Loop0
EndLoop0:
	new Lazysort
	dup
	astore 7
	aload 7
	invokespecial Lazysort.<init>()V
	aload 7
	aload_1
	invokevirtual Lazysort.quicksort([I)Z
	pop
	aload 7
	aload_1
	invokevirtual Lazysort.printL([I)Z
	istore 9
	return
.end method

.method public quicksort([I)Z
	.limit stack 5
	.limit locals 9

	iconst_0
	iconst_5
	invokestatic MathUtils.random(II)I
	istore_2
	iload_2
	iconst_4
	if_icmplt True1
	iconst_0
	goto Store1
True1:
	iconst_1
Store1:
	istore 4
	iload 4
	iload 4
	if_icmpne else1
	aload_0
	aload_1
	invokevirtual Lazysort.beLazy([I)Z
	pop
	iconst_1
	istore 5
	goto endif1
else1:
	iconst_0
	istore 5
endif1:
	iload 5
	iload 5
	if_icmpne else2
	iload 5
	ifeq True2
	iconst_0
	goto Store2
True2:
	iconst_1
Store2:
	istore 5
	goto endif2
else2:
	aload_1
	arraylength
	istore 6
	iload 6
	iconst_1
	isub
	istore 7
	aload_0
	aload_1
	iconst_0
	iload 7
	invokevirtual Lazysort.quicksort([III)Z
	istore 5
endif2:
	iload 5
	ireturn
.end method

.method public beLazy([I)Z
	.limit stack 4
	.limit locals 10

	aload_1
	arraylength
	istore_2
	iload_2
	istore_3
	iconst_0
	istore 4
Loop3:
	iload_3
	iconst_2
	idiv
	istore 5
	iload 4
	iload 5
	if_icmplt True1
	iconst_0
	goto Store1
True1:
	iconst_1
Store1:
	istore 6
	iload 6
	iload 6
	if_icmpeq Body3
	goto EndLoop3
Body3:
	aload_1
	iload 4
	iconst_0
	bipush 10
	invokestatic MathUtils.random(II)I
	iastore
	iload 4
	iconst_1
	iadd
	istore 4
	goto Loop3
EndLoop3:
Loop4:
	iload 4
	iload_3
	if_icmplt True2
	iconst_0
	goto Store2
True2:
	iconst_1
Store2:
	istore 8
	iload 8
	iload 8
	if_icmpeq Body4
	goto EndLoop4
Body4:
	iconst_0
	bipush 10
	invokestatic MathUtils.random(II)I
	istore 9
	aload_1
	iload 4
	iload 9
	iconst_1
	iadd
	iastore
	iload 4
	iconst_1
	iadd
	istore 4
	goto Loop4
EndLoop4:
	iconst_1
	ireturn
.end method
