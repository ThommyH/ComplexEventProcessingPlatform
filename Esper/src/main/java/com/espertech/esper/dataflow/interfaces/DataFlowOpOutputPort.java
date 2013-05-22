/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.dataflow.interfaces;

import com.espertech.esper.dataflow.util.GraphTypeDesc;

public class DataFlowOpOutputPort {
    private final String streamName;
    private final GraphTypeDesc optionalDeclaredType;

    public DataFlowOpOutputPort(String streamName, GraphTypeDesc optionalDeclaredType) {
        this.streamName = streamName;
        this.optionalDeclaredType = optionalDeclaredType;
    }

    public String getStreamName() {
        return streamName;
    }

    public GraphTypeDesc getOptionalDeclaredType() {
        return optionalDeclaredType;
    }
}