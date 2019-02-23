package club.wpia.gigi.testUtils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class ArrayContains<T> extends BaseMatcher<T[]> {

    Matcher<T> containee;

    public ArrayContains(Matcher<T> containee) {
        this.containee = containee;
    }

    @Override
    public boolean matches(Object item) {
        Object[] array = (Object[]) item;
        for (Object t : array) {
            if (containee.matches(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("contains an element that:");
        containee.describeTo(description);
    }

    public static <T> ArrayContains<T> contains(final Matcher<T> element) {
        return new ArrayContains<T>(element);
    }
}
