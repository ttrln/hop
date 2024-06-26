/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.ssh;

import com.trilead.ssh2.Session;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.util.Utils;

public class SessionResult {

  private String stdOut;
  private String stdErr;
  private boolean stdErrorType;

  public SessionResult(Session session) throws HopException {
    readStd(session);
  }

  private void setStdErr(String value) {
    this.stdErr = value;
    if (!Utils.isEmpty(getStdErr())) {
      setStdTypeErr(true);
    }
  }

  public String getStdErr() {
    return this.stdErr;
  }

  public String getStd() {
    return getStdOut() + getStdErr();
  }

  private void setStdOut(String value) {
    this.stdOut = value;
  }

  public String getStdOut() {
    return this.stdOut;
  }

  private void setStdTypeErr(boolean value) {
    this.stdErrorType = value;
  }

  public boolean isStdTypeErr() {
    return this.stdErrorType;
  }

  private void readStd(Session session) throws HopException {
    InputStream isOut = null;
    InputStream isErr = null;
    try {
      isOut = session.getStdout();
      isErr = session.getStderr();

      setStdOut(readInputStream(isOut));
      setStdErr(readInputStream(isErr));

    } catch (Exception e) {
      throw new HopException(e);
    } finally {
      try {
        if (isOut != null) {
          isOut.close();
        }
        if (isErr != null) {
          isErr.close();
        }
      } catch (Exception e) {
        /* Ignore */
      }
    }
  }

  private String readInputStream(InputStream std) throws HopException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(std));

      String line = "";
      StringBuilder stringStdout = new StringBuilder();

      if ((line = br.readLine()) != null) {
        stringStdout.append(line);
      }
      while ((line = br.readLine()) != null) {
        stringStdout.append("\n" + line);
      }

      return stringStdout.toString();
    } catch (Exception e) {
      throw new HopException(e);
    } finally {
      try {
        if (br != null) {
          br.close();
        }
      } catch (Exception e) {
        /* Ignore */
      }
    }
  }
}
