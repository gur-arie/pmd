# Changelog

## ????? - 5.4.0

**New Supported Languages:**

**Feature Request and Improvements:**

**New/Modified/Deprecated Rules:**

*   New Rule: rulesets/java/design.xml/SingleMethodSingletonRule: Verifies that there is only one method called
    "getInstance". If there are more methods that return the singleton, than it can easily happen, that these
    are not the same instances - and thus no singleton.
*   New Rule: rulesets/java/design.xml/SingletonClassReturningNewInstance: Verifies that the method called
    "getInstance" returns a cached instance not always a fresh, new instance.
*   Language Java, ruleset design.xml: The rule "UseSingleton" *has been renamed* to "UseUtilityClass".
    See also bugs [#1059](https://sourceforge.net/p/pmd/bugs/1059) and [#1339](https://sourceforge.net/p/pmd/bugs/1339/).

**Pull Requests:**

*   [#21](https://github.com/adangel/pmd/pull/21): Added PMD Rules for Singleton pattern violations.
*   [#53](https://github.com/pmd/pmd/pull/53): Fix some NullPointerExceptions

**Bugfixes:**

*   [#1332](https://sourceforge.net/p/pmd/bugs/1332/): False Positive: UnusedPrivateMethod
*   [#1333](https://sourceforge.net/p/pmd/bugs/1333/): Error while processing Java file with Lambda expressions
*   [#1337](https://sourceforge.net/p/pmd/bugs/1337/): False positive "Avoid throwing raw exception types" when exception is not thrown
*   [#1338](https://sourceforge.net/p/pmd/bugs/1338/): The pmd-java8 POM bears the wrong parent module version

**API Changes:**
