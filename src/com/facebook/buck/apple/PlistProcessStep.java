/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.apple;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.google.common.collect.ImmutableMap;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.file.Path;

public class PlistProcessStep implements Step {

  private final Path input;
  private final Path output;
  private final ImmutableMap<String, NSObject> additionalKeys;
  private final ImmutableMap<String, NSObject> overrideKeys;

  public PlistProcessStep(
      Path input,
      Path output,
      ImmutableMap<String, NSObject> additionalKeys,
      ImmutableMap<String, NSObject> overrideKeys) {
    this.input = input;
    this.output = output;
    this.additionalKeys = additionalKeys;
    this.overrideKeys = overrideKeys;
  }

  @Override
  public int execute(ExecutionContext context) throws InterruptedException {
    ProjectFilesystem filesystem = context.getProjectFilesystem();
    try (InputStream stream = filesystem.newFileInputStream(input);
         BufferedInputStream bufferedStream = new BufferedInputStream(stream)) {
      NSDictionary infoPlist;
      try {
        infoPlist = (NSDictionary) PropertyListParser.parse(bufferedStream);
      } catch (Exception e) {
        throw new IOException(e);
      }

      for (ImmutableMap.Entry<String, NSObject> entry : additionalKeys.entrySet()) {
        if (!infoPlist.containsKey(entry.getKey())) {
          infoPlist.put(entry.getKey(), entry.getValue());
        }
      }

      infoPlist.putAll(overrideKeys);

      String serializedInfoPlist = infoPlist.toXMLPropertyList();
      filesystem.writeContentsToPath(
          serializedInfoPlist,
          output);
    } catch (IOException e) {
      context.logError(e, "error parsing plist %s", input);
      return 1;
    }

    return 0;
  }

  @Override
  public String getShortName() {
    return "process-plist";
  }

  @Override
  public String getDescription(ExecutionContext context) {
    return String.format("process-plist %s %s", input, output);
  }

}
