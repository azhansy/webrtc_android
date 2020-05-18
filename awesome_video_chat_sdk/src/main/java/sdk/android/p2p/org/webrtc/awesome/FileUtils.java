package org.webrtc.awesome;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * @author dashu
 * @date 5/3/20
 * describe:
 */
public class FileUtils {
    public static final long SIZE_1M = 1024 * 1024;

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        id = id.replaceFirst("raw:", "");
                    }
                    if (id.startsWith("msf:")) {
                        return null;
                    }

                    try {
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                        return getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * *
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 读取文件内容, 失败将返回空串
     */
    public static byte[] readBytes(String filepath) {
        Log.d("azhansy", String.format("read %s", filepath));
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filepath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int len = fis.read(buffer, 0, buffer.length);
                if (len == -1) {
                    break;
                } else {
                    baos.write(buffer, 0, len);
                }
            }
            byte[] data = baos.toByteArray();
            baos.close();
            return data;
        } catch (IOException e) {
            Log.d("azhansy", "e=" + e.getMessage());

            return null;
        } finally {
            closeSilently(fis);
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            // do nothing
        }
    }

    /**
     * description:  将流转换成String
     *
     * @param filePath
     * @return
     * @author xyc
     * @update 2015-6-4
     */
    public static String read2String(String filePath) {
        String ioStr = "";
        FileInputStream is = null;
        try {
            is = new FileInputStream(filePath);
            byte[] in = new byte[is.available()];
            is.read(in);
            ioStr = HQCodec.hexEncode(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ioStr;
    }

    /**
     * description:  将文件流写到某一个目录下
     *
     * @param ioStr
     * @param filePath
     * @author xyc
     * @update 2015-6-4
     */
    public static void read2Io(String ioStr, String filePath) {
        FileOutputStream fos = null;
        try {
            byte[] iobyte = HQCodec.hexDecode(ioStr);
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);
            fos.write(iobyte, 0, iobyte.length);
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] getBlock(long offset, File file, int blockSize) {
        byte[] result = new byte[blockSize];
        RandomAccessFile accessFile = null;
        try {
            accessFile = new RandomAccessFile(file, "r");
            accessFile.seek(offset);
            int readSize = accessFile.read(result);
            if (readSize == -1) {
                return null;
            } else if (readSize == blockSize) {
                return result;
            } else {
                byte[] tmpByte = new byte[readSize];
                System.arraycopy(result, 0, tmpByte, 0, readSize);
                return tmpByte;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (accessFile != null) {
                try {
                    accessFile.close();
                } catch (IOException e1) {
                }
            }
        }
        return null;
    }


    public static String ROOT_PATH = "webrtc/";

    //获取文件存放根路径
    public static File getAppDir(Context context) {
        String dirPath = "";
        //SD卡是否存在
        boolean isSdCardExists = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        boolean isRootDirExists = Environment.getExternalStorageDirectory().exists();
        if (isSdCardExists && isRootDirExists) {
            dirPath = String.format("%s/%s/", Environment.getExternalStorageDirectory().getAbsolutePath(), ROOT_PATH);
        } else {
            dirPath = String.format("%s/%s/", context.getApplicationContext().getFilesDir().getAbsolutePath(), ROOT_PATH);
        }

        File appDir = new File(dirPath);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        return appDir;
    }


    /**
     * 文件分批写入
     *
     * @param fileName    文件路径
     * @param inputStream 需要写入的内容
     * @param start       写入的初始位置
     */
    public synchronized static void appendFileWithInstream(String fileName, InputStream inputStream, long start) {
        File file = new File(fileName);
        RandomAccessFile randomAccessFile = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            randomAccessFile = new RandomAccessFile(file, "rw");
            final long M = 1024;
            byte[] bytes = new byte[(int) M];
            long seek = start;
            long total = 0;
            for (; ; ) {
                int readCount = inputStream.read(bytes);

                if (readCount == 0) {
                    continue;
                }
                if (readCount == -1) {
                    Log.d("azhansy", "readCount = " + readCount + "\n start = " + start + "\ntotal = " + total);
                    Log.d("azhansy", "fileLength = " + randomAccessFile.length()+ "\n seek = " + seek );
                    inputStream.close();
                    break;
                }
                total += readCount;
                randomAccessFile.seek(seek );
                randomAccessFile.write(bytes, 0, readCount);
                seek += readCount;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
