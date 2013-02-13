package com.webobjects.appserver._private;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketException;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableData;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableRange;
import com.webobjects.foundation.NSRange;
import com.webobjects.foundation._NSStringUtilities;

public final class WOHttpIO
{
//  private static final int USE_KEEP_ALIVE_DEFAULT = 2;
  private int _keepAlive;
	// private static final int _TheInputBufferSize = 2048;
	// private static final int _TheOutputBufferSize = 2048;
  private static final int _HighWaterBufferSize;
  public static String URIResponseString = " Apple WebObjects\r\n";
  private final WOHTTPHeaderValue KeepAliveValue = new WOHTTPHeaderValue("keep-alive");
  private final WOHTTPHeaderValue CloseValue = new WOHTTPHeaderValue("close");
  private final WOLowercaseCharArray ConnectionKey = new WOLowercaseCharArray("connection");
  private final WOLowercaseCharArray ContentLengthKey = new WOLowercaseCharArray("content-length");
  private final WOLowercaseCharArray TransferEncodingKey = new WOLowercaseCharArray("transfer-encoding");
  private byte[] _buffer;
  private int _bufferLength;
  private int _bufferIndex;
  private int _lineStartIndex;
  StringBuffer _headersBuffer;
  public boolean _socketClosed;
  private final WOApplication _application = WOApplication.application();
  private static boolean _expectContentLengthHeader = true;
  private static int _contentTimeout = 5000;
  private final WOHTTPHeadersDictionary _headers = new WOHTTPHeadersDictionary();
  public static boolean _alwaysAppendContentLength = true;

  static
  {
    int value = Integer.getInteger("WOMaxIOBufferSize", 8196).intValue();
    if (value != 0)
      _HighWaterBufferSize = value >= 2048 ? value : 2048;
    else
      _HighWaterBufferSize = 8196;
  }

  public static void expectContentLengthHeader(boolean expectContentLengthHeader, int contentTimeout)
  {
    _expectContentLengthHeader = expectContentLengthHeader;
    _contentTimeout = contentTimeout;
  }

  private int _readBlob(InputStream inputStream, int length)
    throws IOException
  {
    byte[] leftOverBuffer = this._buffer;
    int leftOverLength = this._bufferLength - this._bufferIndex;
    int leftOverStart = this._bufferIndex;
    _ensureBufferIsLargeEnoughToRead(length - leftOverLength);
    if (this._buffer != leftOverBuffer)
    {
      System.arraycopy(leftOverBuffer, leftOverStart, this._buffer, 0, leftOverLength);
      this._bufferLength = leftOverLength;
    }
    int read = leftOverLength;
    for (int newlyRead = 1; (read < length) && (newlyRead > 0); read += newlyRead) {
      newlyRead = inputStream.read(this._buffer, this._bufferIndex + read, length - read);
    }
    return read <= length ? read : length;
  }

  private int refillInputBuffer(InputStream inputStream)
    throws IOException
  {
    int moreLength = 0;
    boolean resetLineStartIndex = true;
    if (this._bufferIndex >= 1)
    {
      if (this._bufferLength < this._buffer.length)
      {
        moreLength = inputStream.read(this._buffer, this._bufferLength, this._buffer.length - this._bufferLength);
        resetLineStartIndex = false;
      }
      else {
        byte[] leftOverBuffer = this._buffer;
        int leftOverLength = this._bufferLength - this._lineStartIndex;
        int leftOverLineStartIndex = this._lineStartIndex;
        _ensureBufferIsLargeEnoughToRead(this._buffer.length);
        System.arraycopy(leftOverBuffer, leftOverLineStartIndex, this._buffer, 0, leftOverLength);
        this._bufferLength = leftOverLength;
        moreLength = inputStream.read(this._buffer, leftOverLength, this._buffer.length - leftOverLength);
        this._bufferIndex = leftOverLength;
      }
    }
    else {
      this._bufferLength = 0;
      this._bufferIndex = 0;
      moreLength = inputStream.read(this._buffer, 0, this._buffer.length);
    }
    if (moreLength < 1)
      return 0;
    this._bufferLength += moreLength;
    if (resetLineStartIndex)
      this._lineStartIndex = 0;
    return this._bufferLength;
  }

  public int readLine(InputStream inputStream)
    throws IOException
  {
    boolean foundNewline = false;
    boolean foundCR = false;
    boolean foundEnd = false;
    this._lineStartIndex = this._bufferIndex;
    do
    {
      while (this._bufferIndex < this._bufferLength)
      {
        if (foundNewline)
        {
          if (this._buffer[this._bufferIndex] == 9)
          {
            this._buffer[this._bufferIndex] = 32;
            foundNewline = foundCR = false;
          }
          else if (this._buffer[this._bufferIndex] == 32) {
            foundNewline = foundCR = false;
          } else {
            foundEnd = true;
          }
        } else if (this._buffer[this._bufferIndex] == 13)
        {
          this._buffer[this._bufferIndex] = 32;
          foundCR = true;
        }
        else if (this._buffer[this._bufferIndex] == 10)
        {
          this._buffer[this._bufferIndex] = 32;
          foundNewline = true;
          if (this._bufferIndex - this._lineStartIndex < 2)
          {
            foundEnd = true;
            this._bufferIndex += 1;
          }
        }
        if (foundEnd)
          break;
        this._bufferIndex += 1;
      }
      if ((this._bufferIndex < this._bufferLength) || (foundEnd) || (refillInputBuffer(inputStream) != 0))
        continue;
      if (foundNewline) break;
      return 0;
    }
    while (!
      foundEnd);
    int endSearchLocation = this._bufferIndex;
    if (this._bufferIndex > this._bufferLength)
      this._bufferIndex = this._bufferLength;
    if (foundNewline)
    {
      endSearchLocation--;
      if (foundCR)
        endSearchLocation--;
    }
    return endSearchLocation - this._lineStartIndex;
  }

  public WOHttpIO()
  {
    this._socketClosed = false;
    this._buffer = new byte[2048];
    this._headersBuffer = new StringBuffer(2048);
  }

  public void resetBuffer()
  {
    this._bufferLength = 0;
    this._bufferIndex = 0;
    this._lineStartIndex = 0;
  }

  private void _ensureBufferIsLargeEnoughToRead(int length)
  {
    int newSize = this._buffer.length;
    if (length + this._bufferLength > newSize)
    {
      while (length + this._bufferLength > newSize) newSize <<= 1;
      this._buffer = new byte[newSize];
      resetBuffer();
    }
  }

  private void _shrinkBufferToHighWaterMark()
  {
    if (this._buffer.length > _HighWaterBufferSize)
    {
      this._buffer = new byte[2048];
      resetBuffer();
    }
  }

  public WORequest readRequestFromSocket(Socket connectionSocket)
    throws IOException
  {
    InputStream sis = connectionSocket.getInputStream();
    int p = 0;
    int q = 0;
    int offset = 0;
    WORequest aRequest = null;
    String aMethodString = null;
    String aURIString = null;
    String aHttpVersionString = null;
    resetBuffer();
    this._headers.dispose();
    int lineLength = readLine(sis);
    if (lineLength == 0)
      return null;
    offset = this._lineStartIndex;

    int lineLengthMinusOne;
    for (lineLengthMinusOne = lineLength - 1; (this._buffer[(p + offset)] != 32) && (p < lineLengthMinusOne); p++){}
    if (p < lineLengthMinusOne)
    {
      for (q = lineLengthMinusOne; (this._buffer[(q + offset)] != 32) && (q > p); q--) {}
      int _stringLength = lineLengthMinusOne - q;
      if (_stringLength > 0)
        aHttpVersionString = _NSStringUtilities.stringForBytes(this._buffer, q + offset + 1, _stringLength, WORequest.defaultHeaderEncoding());
      _stringLength = q - p - 1;
      if (_stringLength > 0)
        aURIString = _NSStringUtilities.stringForBytes(this._buffer, p + offset + 1, _stringLength, WORequest.defaultHeaderEncoding());
      _stringLength = p;
      if (_stringLength > 0)
        aMethodString = _NSStringUtilities.stringForBytes(this._buffer, offset, _stringLength, WORequest.defaultHeaderEncoding());
    }
    this._keepAlive = 2;
    InputStream pbsis = _readHeaders(sis, true, true, false);
    NSData contentData = null;
    int contentLengthInt = 0;
    NSArray headers = (NSArray)this._headers.objectForKey(this.ContentLengthKey);
    if ((headers != null) && (headers.count() == 1) && (pbsis != null))
    {
      try
      {
        contentLengthInt = Integer.parseInt(headers.lastObject().toString());
      }
      catch (NumberFormatException e)
      {
        if (WOApplication._isDebuggingEnabled())
          NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + headers.lastObject() + "'.");
      }
      if (contentLengthInt > 0)
        contentData = new WOInputStreamData(pbsis, contentLengthInt);
    }
    else {
      NSData fakeContentData = _content(sis, connectionSocket, false);
      if (fakeContentData != null)
        contentData = new WOInputStreamData(fakeContentData);
    }
    aRequest = this._application.createRequest(aMethodString, aURIString, aHttpVersionString, this._headers == null ? null : this._headers.headerDictionary(), contentData, null);
    if (aRequest != null)
    {
      aRequest._setOriginatingAddress(connectionSocket.getInetAddress());
      aRequest._setOriginatingPort(connectionSocket.getPort());
      aRequest._setAcceptingAddress(connectionSocket.getLocalAddress());
      aRequest._setAcceptingPort(connectionSocket.getLocalPort());
    }
    _shrinkBufferToHighWaterMark();
    return aRequest;
  }

  private void appendMessageHeaders(WOMessage message)
  {
    NSDictionary headers = message.headers();
    if (headers != null)
    {
      if (!(headers instanceof NSMutableDictionary))
        headers = headers.mutableClone();
      ((NSMutableDictionary)headers).removeObjectForKey(this.ContentLengthKey);
      NSArray headerKeys = headers.allKeys();
      int kc = headerKeys.count();
      for (int i = 0; i < kc; i++)
      {
        Object aKey = headerKeys.objectAtIndex(i);
        NSArray values = message.headersForKey(aKey);
        int vc = values.count();
        if ((aKey instanceof WOLowercaseCharArray))
        {
          char[] aKeyCharArray = ((WOLowercaseCharArray)aKey).toCharArray();
          for (int j = 0; j < vc; j++)
          {
            this._headersBuffer.append(aKeyCharArray);
            this._headersBuffer.append(": ");
            this._headersBuffer.append(values.objectAtIndex(j));
            this._headersBuffer.append("\r\n");
          }
        }
        else
        {
          for (int j = 0; j < vc; j++)
          {
            this._headersBuffer.append(aKey);
            this._headersBuffer.append(": ");
            this._headersBuffer.append(values.objectAtIndex(j));
            this._headersBuffer.append("\r\n");
          }
        }
      }
    }
  }

  public boolean sendResponse(WOResponse aResponse, Socket connectionSocket, WORequest aRequest)
    throws IOException
  {
    String httpVersion = aResponse.httpVersion();
    this._headersBuffer.setLength(0);
    this._headersBuffer.append(httpVersion);
    this._headersBuffer.append(' ');
    this._headersBuffer.append(aResponse.status());
    this._headersBuffer.append(URIResponseString);
    return sendMessage(aResponse, connectionSocket, httpVersion, aRequest);
  }

  public void sendRequest(WORequest aRequest, Socket connectionSocket)
    throws IOException
  {
    String httpVersion = aRequest.httpVersion();
    this._headersBuffer.setLength(0);
    this._headersBuffer.append(aRequest.method());
    this._headersBuffer.append(' ');
    this._headersBuffer.append(aRequest.uri());
    this._headersBuffer.append(' ');
    this._headersBuffer.append(httpVersion);
    this._headersBuffer.append("\r\n");
    sendMessage(aRequest, connectionSocket, httpVersion, null);
  }

  protected boolean sendMessage(WOMessage aMessage, Socket connectionSocket, String httpVersion, WORequest aRequest)
    throws IOException
  {
    long length = 0L;
    NSData someContent = null;

    appendMessageHeaders(aMessage);

    boolean unknownLength = false;

    if ((aMessage instanceof WOResponse))
    {
      WOResponse theResponse = (WOResponse)aMessage;
      InputStream is = theResponse.contentInputStream();
      if ((is != null) && 
        (theResponse.contentInputStreamLength() == 0x7fffffffL || theResponse.contentInputStreamLength() == -1))
        unknownLength = true;
    }
    boolean keepSocketAlive;
    if (unknownLength)
    {
      this._headersBuffer.append("connection: close\r\n");
      keepSocketAlive = false;
    }
    else
    {
      if (isHTTP11(httpVersion))
      {
        if (this._keepAlive == 0) {
          this._headersBuffer.append("connection: close\r\n");
          keepSocketAlive = false;
        } else {
          keepSocketAlive = true;
        }
      }
      else
      {
        if (this._keepAlive == 1) {
          this._headersBuffer.append("connection: keep-alive\r\n");
          keepSocketAlive = true;
        } else {
          keepSocketAlive = false;
        }
      }
    }
    if (aRequest != null) {
      NSData contentData = aRequest.content();
      if ((contentData != null) && ((contentData instanceof WOInputStreamData))) {
        WOInputStreamData isData = (WOInputStreamData)contentData;

        InputStream is = isData._stream();
        if ((is != null) && ((is instanceof WONoCopyPushbackInputStream))) {
          WONoCopyPushbackInputStream sis = (WONoCopyPushbackInputStream)is;
          if (sis.wasPrematurelyTerminated())
          {
            return false;
          }
          String contentLengthString = aRequest.headerForKey("content-length");
          long contentLength = contentLengthString != null ? Long.parseLong(contentLengthString) : 0L;
          if (contentLength > 0L) {
            int _originalReadTimeout = -1;
            try {
              _originalReadTimeout = setSocketTimeout(connectionSocket, _contentTimeout);

              sis.drain();

              if (NSLog.debugLoggingAllowedForLevelAndGroups(3, 4L)) {
                NSLog.out.appendln("<WOHttpIO>: Drained socket");
              }

              if (_originalReadTimeout != -1)
                _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
            }
            catch (SocketException socketException) {
              if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 4L)) {
                NSLog.err.appendln("<WOHttpIO>: Unable to set socket timeout:" + socketException.getMessage());
                NSLog._conditionallyLogPrivateException(socketException);
              }
            } catch (IOException iioe) {
              if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
                NSLog.err.appendln("<WOHttpIO>: Finished reading before content length of " + contentLength + " : " + iioe.getMessage());
                NSLog._conditionallyLogPrivateException(iioe);
              }
            }
          }
        }
      }
    }

    InputStream is = null;
    int bufferSize = 0;

    if ((aMessage instanceof WOResponse))
    {
      WOResponse theResponse = (WOResponse)aMessage;
      is = theResponse.contentInputStream();
      if (is != null) {
        bufferSize = theResponse.contentInputStreamBufferSize();
        length = theResponse.contentInputStreamLength();
      }
    }

    if (is == null) {
      someContent = aMessage.content();
      length = someContent.length();
    }

    if ((!unknownLength) && ((_alwaysAppendContentLength) || (length > 0L))) {
      this._headersBuffer.append("content-length: ");
      this._headersBuffer.append(length);
      this._headersBuffer.append("\r\n");
    }

    this._headersBuffer.append("\r\n");

    OutputStream outputStream = connectionSocket.getOutputStream();

    byte[] headerBytes = _NSStringUtilities.bytesForIsolatinString(new String(this._headersBuffer));
    outputStream.write(headerBytes, 0, headerBytes.length);

    String method = aRequest != null ? aRequest.method() : "";
    boolean isHead = method.equals("HEAD");
    if ((unknownLength) || ((length > 0L) && (!isHead))) {
      if (is == null) {
        NSMutableRange range = new NSMutableRange();
        byte[] contentBytesNoCopy = someContent != null ? someContent.bytesNoCopy(range) : new byte[0];
        outputStream.write(contentBytesNoCopy, range.location(), range.length());
      }
      else {
        try {
          byte[] buffer = new byte[bufferSize];

          if (unknownLength) {
            while (true)
            {
              int read = is.read(buffer, 0, bufferSize);
              if (read == -1) {
                break;
              }
              outputStream.write(buffer, 0, read);
            }
          }

          do
          {
            int read = is.read(buffer, 0, length > bufferSize ? bufferSize : (int)length);
            if (read == -1)
              break;
            length -= read;
            outputStream.write(buffer, 0, read);
          }
          while (length > 0L);
        }
        finally
        {
          try
          {
            is.close();
          } catch (Exception e) {
            NSLog.err.appendln("<WOHttpIO>: Failed to close content InputStream: " + e);
            if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
              NSLog.err.appendln(e);
            }
          }
        }
      }
    }
    outputStream.flush();

    return keepSocketAlive;
  }

  public WOResponse readResponseFromSocket(Socket connectionSocket)
    throws IOException
  {
    InputStream sis = connectionSocket.getInputStream();
    int p = 0;
    int q = 0;
    int offset = 0;
    WOResponse response = null;
    String statusCode = null;
    String httpVersion = null;
    resetBuffer();
    int lineLength = readLine(sis);
    if (lineLength == 0)
      return null;
    _NSStringUtilities.stringForBytes(this._buffer, offset, lineLength, WORequest.defaultHeaderEncoding());
    offset = this._lineStartIndex;

    int lineLengthMinusOne;
    for (lineLengthMinusOne = lineLength - 1; (this._buffer[(p + offset)] != 32) && (p < lineLengthMinusOne); p++){}
    if (p < lineLengthMinusOne)
    {
      for (q = p + 1; (this._buffer[(q + offset)] != 32) && (q < lineLengthMinusOne); q++){}
      if (q < lineLengthMinusOne)
        _NSStringUtilities.stringForBytes(this._buffer, q + offset + 1, lineLengthMinusOne - q, WORequest.defaultHeaderEncoding());
      statusCode = _NSStringUtilities.stringForBytes(this._buffer, p + offset + 1, q - p - 1, WORequest.defaultHeaderEncoding());
      httpVersion = _NSStringUtilities.stringForBytes(this._buffer, offset, p, WORequest.defaultHeaderEncoding());
    }
    if (this._application != null)
      response = this._application.createResponseInContext(null);
    else
      response = new WOResponse();
    response.setHTTPVersion(httpVersion);
    response.setStatus(Integer.parseInt(statusCode));
    _readHeaders(sis, false, false, false);
    response._setHeaders(this._headers);
    boolean closeConnection = false;
    NSArray connectionStatus = (NSArray)this._headers.valueForKey("Connection");
    if (connectionStatus != null)
    {
      int count = connectionStatus.count();
      int i = 0;

      while (i < count)
      {
        String headerValue = (String)connectionStatus.objectAtIndex(i);
        if (headerValue.equalsIgnoreCase("close"))
        {
          closeConnection = true;
          break;
        }
        i++;
      }
    }
    NSData contentData = _content(sis, connectionSocket, closeConnection);
    response.setContent(contentData);
    _shrinkBufferToHighWaterMark();
    if ((closeConnection) || ((isHTTP11(httpVersion)) && (this._keepAlive == 0)) || ((!isHTTP11(httpVersion)) && (this._keepAlive != 1)))
    {
      connectionSocket.close();
      this._socketClosed = true;
    }
    return response;
  }

  private static final boolean isHTTP11(String httpVersion)
  {
    return (httpVersion != null) && ("HTTP/1.1".equals(httpVersion));
  }

  public NSDictionary headers()
  {
    return this._headers;
  }

  public InputStream _readHeaders(InputStream sis, boolean checkKeepAlive, boolean isRequest, boolean isMultipartHeaders)
    throws IOException
  {
    int offset = 0;
    while (true)
    {
      int lineLength = readLine(sis);
      if (lineLength == 0)
        break;
      offset = this._lineStartIndex;
      int startValue = 0;
      int separator = 0;
      for (int i = 0; i < lineLength; i++)
      {
        if (this._buffer[(offset + i)] != 58)
          continue;
        separator = i;
        for (i++; (i < lineLength) && (this._buffer[(offset + i)] == 32); i++){}
        if (i >= lineLength)
          continue;
        startValue = i;
        break;
      }

      if (startValue == 0)
        continue;
      int key_offset = offset;
      int key_length = separator;
      int value_offset = offset + startValue;
      int value_length = lineLength - startValue;
      WOHTTPHeaderValue headerValue = this._headers.setBufferForKey(this._buffer, value_offset, value_length, key_offset, key_length);
      WOLowercaseCharArray headerKey = this._headers.lastInsertedKey();
      if ((checkKeepAlive) && (this._keepAlive == 2) && (this.ConnectionKey.equals(headerKey))) {
        if (headerValue.equalsIgnoreCase(this.KeepAliveValue)) {
          this._keepAlive = 1; continue;
        }
        if (headerValue.equalsIgnoreCase(this.CloseValue))
          this._keepAlive = 0;
      }
    }
    WONoCopyPushbackInputStream pbsis = null;
    int pushbackLength = this._bufferLength - this._bufferIndex;
    if (isRequest)
    {
      int contentLengthInt = 0;
      NSArray headers = (NSArray)this._headers.objectForKey(this.ContentLengthKey);
      if ((headers != null) && (headers.count() == 1))
      {
        try
        {
          contentLengthInt = Integer.parseInt(headers.lastObject().toString());
        }
        catch (NumberFormatException e)
        {
          if (WOApplication._isDebuggingEnabled())
            NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + headers.lastObject() + "'.");
        }
        if (pushbackLength > contentLengthInt)
        {
          contentLengthInt = pushbackLength;
          this._headers.setObjectForKey(new NSMutableArray("" + pushbackLength), this.ContentLengthKey);
        }
        pbsis = new WONoCopyPushbackInputStream(new BufferedInputStream(sis), contentLengthInt - pushbackLength);
      }
    }
    else if ((isMultipartHeaders) && ((sis instanceof WONoCopyPushbackInputStream))) {
      pbsis = (WONoCopyPushbackInputStream)sis;
    }if ((pbsis != null) && (pushbackLength > 0))
      pbsis.unread(this._buffer, this._bufferIndex, pushbackLength);
    return pbsis;
  }

  private NSData _forceReadContent(InputStream sis, Socket connectionSocket)
  {
    int bytesRead = 0;
    NSMutableData _contentData = null;
    BufferedInputStream bis = new BufferedInputStream(sis);
    byte[] buffer = new byte[2048];

    int _originalReadTimeout = setSocketTimeout(connectionSocket, _contentTimeout);

    if (this._bufferLength > this._bufferIndex) {
      _contentData = new NSMutableData(this._bufferLength - this._bufferIndex);
      _contentData.appendBytes(this._buffer, new NSRange(this._bufferIndex, this._bufferLength - this._bufferIndex));
    } else {
      _contentData = new NSMutableData();
    }
    while (true)
    {
      try {
        bytesRead = bis.read(buffer, 0, 2048);
        if (bytesRead >= 0) {
          _contentData.appendBytes(buffer, new NSRange(0, bytesRead));
        }
        else
        {
          if (_originalReadTimeout == -1) {
        	  return _contentData;
          }
          _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
          NSMutableData localNSMutableData1 = _contentData;
          return localNSMutableData1;
        }
      }
      catch (IOException ioException)
      {
        if (NSLog.debugLoggingAllowedForLevelAndGroups(2, 4L)) {
          NSLog.err.appendln("<WOHttpIO>: IOException occurred during read():" + ioException.getMessage());
          NSLog._conditionallyLogPrivateException(ioException);
        }
        return null;
      }
      finally
      {
        if (_originalReadTimeout != -1)
          _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
      }
      if (_originalReadTimeout != -1)
        _originalReadTimeout = setSocketTimeout(connectionSocket, _originalReadTimeout);
    }
  }

  private NSData _content(InputStream sis, Socket connectionSocket, boolean connectionClosed)
    throws IOException
  {
    byte[] content = null;
    int length = 0;
    int offset = 0;
    NSData contentData = null;
    NSMutableArray contentLength = (NSMutableArray)this._headers.objectForKey(this.ContentLengthKey);
    if ((contentLength != null) && (contentLength.count() == 1))
    {
      try
      {
        length = Integer.parseInt((String)contentLength.lastObject());
      }
      catch (NumberFormatException e)
      {
        if (WOApplication._isDebuggingEnabled())
          NSLog.debug.appendln("<" + getClass().getName() + "> Unable to parse content-length header: '" + (String)contentLength.lastObject() + "'.");
      }
      if (length != 0)
      {
        length = _readBlob(sis, length);
        offset = this._bufferIndex;
        if (length > 0)
        {
          content = this._buffer;
        }
        else {
          offset = 0;
          length = 0;
        }
      }
      try
      {
        if (content != null) 
        	contentData = new NSData(content, new NSRange(offset, length), true);
      }
      catch (Exception exception)
      {
        NSLog.err.appendln("<" + getClass().getName() + "> Error: Request creation failed!\n" + exception.toString());
        if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 8196L))
        	NSLog.debug.appendln(exception);
      }
      }
    else
    {
      boolean readChunks = false;
      NSMutableArray encodingKeys = (NSMutableArray)this._headers.objectForKey(this.TransferEncodingKey);
      if ((encodingKeys != null) && (encodingKeys.count() == 1))
      {
        String encoding = (String)encodingKeys.lastObject();
        if ("chunked".equals(encoding))
          readChunks = true;
      }
      if (readChunks) {
        contentData = _readChunks(sis, connectionSocket);
      }
      else if ((connectionClosed) || (!_expectContentLengthHeader))
        contentData = _forceReadContent(sis, connectionSocket);
    }
    return contentData;
  }

  private NSData _readChunks(InputStream is, Socket socket)
    throws IOException
  {
    int _originalReadTimeout = setSocketTimeout(socket, _contentTimeout);
    try
    {
      int bytesInBuffer = this._bufferLength - this._bufferIndex;
      InputStream inputStream = null;
      if (bytesInBuffer > 0)
      {
        inputStream = new PushbackInputStream(is, bytesInBuffer);
        ((PushbackInputStream)inputStream).unread(this._buffer, this._bufferIndex, bytesInBuffer);
      }
      else {
        inputStream = is;
      }
      resetBuffer();
      byte[] buffer = new byte[2048];
      NSMutableData result = new NSMutableData();
      while (true)
      {
        int contentBytesToRead = readChunkSizeLine(inputStream);
        if (contentBytesToRead <= 0)
          break;
        contentBytesToRead += 2; if (contentBytesToRead > buffer.length)
          buffer = new byte[contentBytesToRead];
        int bytesRead = inputStream.read(buffer, 0, contentBytesToRead);
        if (bytesRead > contentBytesToRead)
          bytesRead = contentBytesToRead;
        if (bytesRead > 0)
          result.appendBytes(buffer, new NSRange(0, bytesRead - 2));
      }
      NSMutableData nsmutabledata = result;
      if (_originalReadTimeout != -1)
        _originalReadTimeout = setSocketTimeout(socket, _originalReadTimeout);
      return nsmutabledata;
    }
    catch (IOException exception)
    {
      if (_originalReadTimeout != -1)
        _originalReadTimeout = setSocketTimeout(socket, _originalReadTimeout); 
      throw exception;
    }
  }

  private int readChunkSizeLine(InputStream is)
    throws IOException
  {
    int contentBytesToRead = 0;
    boolean skip = false;
    StringBuffer sb = new StringBuffer();
    while (true)
    {
      int b = is.read();
      sb.append((char)b);
      if (b == 59) {
        skip = true;
      }
      else if (b == 13)
      {
        is.read();
        break;
      }
      if (skip)
        continue;
      int intVal = b < 65 ? b - 48 : (b < 97 ? b - 65 : b - 97) + 10;
      contentBytesToRead *= 16;
      contentBytesToRead += intVal;
    }

    return contentBytesToRead;
  }

  protected int setSocketTimeout(Socket socket, int timeout)
  {
    int old = timeout;
    try
    {
      old = socket.getSoTimeout();
      if (timeout != -1)
        socket.setSoTimeout(timeout);
    }
    catch (SocketException ex)
    {
      if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 8196L))
        NSLog.err.appendln("<WOHttpIO>: Unable to set socket timeout:" + ex.getMessage());
    }
    return old;
  }

  public String toString()
  {
    return "<" + getClass().getName() + " keepAlive='" + this._keepAlive + "' buffer=" + this._buffer + " >";
  }
}