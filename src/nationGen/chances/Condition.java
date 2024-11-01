package nationGen.chances;

import java.util.function.Function;
import nationGen.misc.Arg;
import nationGen.misc.Args;

/**
 * A condition takes a some object and returns a boolean.  Functionally the same as a predicate.
 * @param <T> The type that this condition consumes in order to return a boolean value
 */
public interface Condition<T> {
  boolean test(T f);

  static <T> Condition<T> from(
    Args args,
    Function<Args, Condition<T>> conditionCreator
  ) {
    Condition<T> condition = null;
    ConditionJoiner joiner = null;
    Args runningArgs = new Args();

    for (Arg arg : args) {
      if (arg.isParenthesis()) {
        if (condition != null && joiner == null) {
          throw new IllegalArgumentException(
            String.format(
              "Unexpected parenthesis %s; expected [AND] or [OR] in args %s",
              arg,
              args
            )
          );
        }
        Condition<T> cond = Condition.from(
          arg.getParenthesis(),
          conditionCreator
        );
        if (condition == null) {
          condition = cond;
        } else {
          condition = joiner.join(condition, cond);
          joiner = null;
        }
      } else if (
        arg.get().equalsIgnoreCase("AND") || arg.get().equalsIgnoreCase("OR")
      ) {
        if (joiner != null) {
          if (runningArgs.isEmpty()) {
            throw new IllegalArgumentException(
              String.format(
                "Unexpected condition joiner [%s]; expected condition after [%s] in args %s.",
                arg,
                joiner,
                args
              )
            );
          }
          condition = joiner.join(
            condition,
            conditionCreator.apply(runningArgs)
          );
          runningArgs.clear();
        } else if (condition == null) {
          if (runningArgs.isEmpty()) {
            throw new IllegalArgumentException(
              String.format(
                "Unexpected condition joiner [%s]; expected condition in args %s.",
                arg,
                args
              )
            );
          }
          condition = conditionCreator.apply(runningArgs);
          runningArgs.clear();
        }
        joiner = ConditionJoiner.valueOf(arg.get().toUpperCase());
      } else {
        if (condition != null && joiner == null) {
          throw new IllegalArgumentException(
            String.format(
              "Missing condition joiner after parenthesis in args %s",
              args
            )
          );
        }
        runningArgs.add(arg);
      }
    }
    if (!runningArgs.isEmpty()) {
      Condition<T> last = conditionCreator.apply(runningArgs);
      if (joiner == null) {
        condition = last;
      } else {
        condition = joiner.join(condition, last);
      }
    } else if (joiner != null) {
      throw new IllegalArgumentException(
        String.format(
          "Dangling condition joiner [%s] at end of args %s",
          joiner,
          args
        )
      );
    }
    if (condition == null) {
      throw new IllegalArgumentException("No condition found in args: " + args);
    }
    return condition;
  }
}

enum ConditionJoiner {
  AND {
    @Override
    <T> Condition<T> join(Condition<T> one, Condition<T> two) {
      return t -> one.test(t) && two.test(t);
    }
  },
  OR {
    @Override
    <T> Condition<T> join(Condition<T> one, Condition<T> two) {
      return t -> one.test(t) || two.test(t);
    }
  };

  abstract <T> Condition<T> join(Condition<T> one, Condition<T> two);
}
