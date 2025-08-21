test:
	mill emulator[examples.Adder,SimpleGenerator].mfccompiler.compile

hello:
	mill -i changbaiV1.spinal.runMain changbaiV1.sayHello

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ *.lst changbaiTest/