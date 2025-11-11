package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.JimmerVersion;

class GeneratorVersionChecker {
    static void checkGeneratorVersion(String jimmerVersion, String typeName, String generatorName) {
        if (JimmerVersion.compareVersion(jimmerVersion, JimmerVersion.generationVersion()) < 0) {
            throw new IllegalStateException(
                    "The version of the " + generatorName + " for handling type \"" +
                    typeName +
                    "\" is \"" +
                    jimmerVersion +
                    "\", it cannot be less than \"" +
                    JimmerVersion.generationVersion() +
                    "\" which is the last code generation version of jimmer"
            );
        }
    }
}
