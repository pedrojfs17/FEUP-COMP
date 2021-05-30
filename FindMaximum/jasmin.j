.class FindMaximum
.super java/lang/Object

.field private test_arr [I

.method <init>()V
	.limit stack 1
	.limit locals 1

	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public find_maximum([I)I
	.limit stack 2
	.limit locals 11

	iconst_1
	istore_2
	iconst_0
	istore_3
	aload_1
	iload_3
	iaload
	istore 4
	iload 4
	istore 5
Loop0:
	aload_1
	arraylength
	istore 6
	iload_2
	iload 6
	if_icmplt True1
	iconst_0
	goto Store1
True1:
	iconst_1
Store1:
	istore 7
	iload 7
	iload 7
	if_icmpne EndLoop0
Body0:
	aload_1
	iload_2
	iaload
	istore 8
	iload 8
	istore 9
	iload 5
	iload 9
	if_icmplt True2
	iconst_0
	goto Store2
True2:
	iconst_1
Store2:
	istore 10
	iload 10
	iload 10
	if_icmpne else1
	iload 9
	istore 5
	goto endif1
else1:
endif1:
	iload_2
	iconst_1
	iadd
	istore_2
	goto Loop0
EndLoop0:
	iload 5
	ireturn
.end method

.method public build_test_arr()I
	.limit stack 4
	.limit locals 14

	iconst_5
	newarray int
	astore_1
	iconst_0
	istore_3
	aload_0
	getfield FindMaximum/test_arr [I
	astore 4
	aload 4
	iload_3
	bipush 14
	iastore
	iconst_1
	istore 5
	aload_0
	getfield FindMaximum/test_arr [I
	astore 6
	aload 6
	iload 5
	bipush 28
	iastore
	iconst_2
	istore 7
	aload_0
	getfield FindMaximum/test_arr [I
	astore 8
	aload 8
	iload 7
	iconst_0
	iastore
	iconst_3
	istore 9
	aload_0
	getfield FindMaximum/test_arr [I
	astore 10
	aload 10
	iload 9
	iconst_0
	iconst_5
	isub
	iastore
	iconst_4
	istore 11
	aload_0
	getfield FindMaximum/test_arr [I
	astore 12
	aload 12
	iload 11
	bipush 12
	iastore
	iconst_0
	ireturn
.end method

.method public get_array()[I
	.limit stack 1
	.limit locals 4

	aload_0
	getfield FindMaximum/test_arr [I
	astore_1
	aload_1
	areturn
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 3
	.limit locals 7

	new FindMaximum
	dup
	astore_1
	aload_1
	invokespecial FindMaximum.<init>()V
	aload_1
	invokevirtual FindMaximum.build_test_arr()I
	pop
	aload_1
	invokevirtual FindMaximum.get_array()[I
	astore_3
	aload_1
	aload_3
	invokevirtual FindMaximum.find_maximum([I)I
	istore 4
	iload 4
	invokestatic ioPlus.printResult(I)V
	return
.end method
