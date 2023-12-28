// Copyright 2020 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.packages;

import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;

/**
 * Utility class encapsulating the standard definition of the {@code package()} function of BUILD
 * files.
 */
public class PackageCallable {

  protected PackageCallable() {}

  public static final PackageCallable INSTANCE = new PackageCallable();

  @StarlarkMethod(
      name = "package",
      documented = false, // documented in docgen/templates/be/functions.vm
      extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
      useStarlarkThread = true)
  public Object packageCallable(Map<String, Object> kwargs, StarlarkThread thread)
      throws EvalException {
    Package.Builder pkgBuilder = PackageFactory.getContext(thread);
    if (pkgBuilder.isPackageFunctionUsed()) {
      throw new EvalException("'package' can only be used once per BUILD file");
    }
    pkgBuilder.setPackageFunctionUsed();

    if (kwargs.isEmpty()) {
      throw new EvalException("at least one argument must be given to the 'package' function");
    }

    PackageArgs.Builder pkgArgsBuilder = PackageArgs.builder();
    boolean disallowDistribs =
        thread.getSemantics().getBool(BuildLanguageOptions.INCOMPATIBLE_NO_PACKAGE_DISTRIBS);
    for (Map.Entry<String, Object> kwarg : kwargs.entrySet()) {
      String name = kwarg.getKey();
      if (disallowDistribs && name.equals("distribs")) {
        throw Starlark.errorf(
            "'package(distribs=...)' is not allowed when --incompatible_no_package_distribs is"
                + " set");
      }
      Object rawValue = kwarg.getValue();
      processParam(name, rawValue, pkgBuilder, pkgArgsBuilder);
    }
    pkgBuilder.mergePackageArgsFrom(pkgArgsBuilder);
    return Starlark.NONE;
  }

  /**
   * Handles one parameter. Subclasses can add new parameters by overriding this method and falling
   * back on the super method when the parameter does not match.
   */
  protected void processParam(
      String name, Object rawValue, Package.Builder pkgBuilder, PackageArgs.Builder pkgArgsBuilder)
      throws EvalException {

    PackageArgs.processParam(
        name,
        rawValue,
        "package() argument '" + name + "'",
        pkgBuilder.getLabelConverter(),
        pkgArgsBuilder);
  }
}
