package app;

import magnet.Instance;
import magnet.Scope;
import magnet.Scoping;

@Instance(
    type = UnderTest.class,
    scoping = Scoping.UNSCOPED,
    limitedTo = "activity"
)
public class UnderTest {
    public UnderTest(Scope scope) {
    }
}