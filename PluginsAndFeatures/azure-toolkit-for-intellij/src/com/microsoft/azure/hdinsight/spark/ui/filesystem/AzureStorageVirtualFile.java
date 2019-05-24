package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.http.HttpFileSystemImpl;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileInfo;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public abstract class AzureStorageVirtualFile extends HttpVirtualFile implements ILogger {
    abstract public void setParent(VirtualFile parent);
    @Nullable
    @Override
    public RemoteFileInfo getFileInfo() {
        // unimplemented method
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException("Adls Gen2 FileSystem is read-only");
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        // unimplemented method
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        // unimplemented method
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
        // unimplemented method
    }

    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("unimplemented method for Adls Gen2 FileSystem");
    }
}
