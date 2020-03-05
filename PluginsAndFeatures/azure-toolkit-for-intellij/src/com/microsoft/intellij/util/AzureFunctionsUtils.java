/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.microsoft.intellij.util;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.template.FunctionTemplate;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.intellij.runner.functions.core.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AzureFunctionsUtils {
    private static List<FunctionTemplate> functionTemplates;
    private static final String DOUBLE_QUOTE = "\"";

    public static FunctionTemplate getFunctionTemplate(String trigger) throws AzureExecutionException {
        if (functionTemplates == null) {
            functionTemplates = FunctionUtils.loadAllFunctionTemplates();
        }
        return functionTemplates.stream()
                .filter(template -> StringUtils.equalsIgnoreCase(trigger, template.getFunction()))
                .findFirst().orElseThrow(() -> new AzureExecutionException("No such template"));
    }

    public static void applyKeyValueToLocalSettingFile(File localSettingFile, String key, String value) throws IOException {
        if (!localSettingFile.getParentFile().isDirectory()) {
            throw new IOException("Cannot save file to a non-existing directory: " + localSettingFile.getParent());
        }
        final JsonObject localSettingRoot = localSettingFile.exists() ?
                JsonUtils.readJsonFile(localSettingFile) : new JsonObject();
        if (localSettingRoot.has("IsEncrypted")) {
            localSettingRoot.add("IsEncrypted", new JsonPrimitive(false));
        }
        JsonObject appSettings = localSettingRoot.getAsJsonObject("Values");
        if (appSettings == null) {
            appSettings = new JsonObject();
            localSettingRoot.add("Values", appSettings);
        }

        appSettings.addProperty(key, value);
        JsonUtils.writeJsonToFile(localSettingFile, localSettingRoot);
    }

    public static String normalizeClassName(String className) {
        if (StringUtils.isBlank(className)) {
            return "untitled";
        }
        final String trimClassName = className.endsWith(".java") ? className.substring(0, className.length() - 5) : className;
        return trimClassName.replaceAll("[^\\.a-zA-Z0-9\\_\\$]+", "").replace('.', '_');
    }

    public static File createMavenProjectToTempFolder(final String groupId, final String artifactId,
                                                      final String version, final String packageName)
            throws IOException, InterruptedException {
        final List<File> mvnCmds = CommandUtils.resolvePathForCommandForCmdOnWindows("mvn");
        if (!mvnCmds.isEmpty()) {
            final File mvnCmd = mvnCmds.get(0);
            final File folder = Files.createTempDir();
            final Map<String, String> maps = new LinkedHashMap<>();
            maps.put("-DarchetypeGroupId", "com.microsoft.azure");
            maps.put("-DarchetypeArtifactId", "azure-functions-archetype");
            maps.put("-DgroupId", groupId);
            maps.put("-DartifactId", artifactId);
            maps.put("-Dversion", version);
            maps.put("-Dpackage", packageName);
            final String args = maps.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(" "));
            CommandUtils.executeMultipleLineOutput(wrapper(mvnCmd.getAbsolutePath()) + " archetype:generate" + " -B " + args, folder);

            return folder;
        }
        return null;
    }

    public static String generateFunctionClassByTrigger(String trigger, String packageName, String className) throws IOException {
        final File tempFile = StreamUtil.getResourceFile(String.format("/azurefunction/templates/%s.template", trigger));
        if (tempFile.exists()) {
            final String templateText = FileUtils.readFileToString(tempFile, Charset.defaultCharset());
            if (StringUtils.isNotBlank(templateText)) {
                return StringUtils.replace(StringUtils.replace(templateText, "$packageName$", packageName), "$className$", className);
            }
        }
        return null;
    }

    public static String substituteParametersInTemplate(final FunctionTemplate template, final Map<String, String> params)
            throws AzureExecutionException {
        String ret = template.getFiles().get("function.java");
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) {
                throw new AzureExecutionException("Required property:" + entry.getKey() + " is missing");
            }
            ret = ret.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return ret;
    }

    private static String wrapper(String file) {
        if (file.contains(" ")) {
            return new StringBuilder().append(DOUBLE_QUOTE).append(file).append(
                DOUBLE_QUOTE).toString();
        }

        return file;
    }
}
