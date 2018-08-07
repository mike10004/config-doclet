package io.github.mike10004.configdoclet;

import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;

import java.io.File;
import java.io.InputStream;

class MyInvoker extends DefaultInvoker {

    private InvocationOutputHandler outputHandlerCopy;

    private InputStream inputStreamCopy;

    private InvocationOutputHandler errorHandlerCopy;

    public MyInvoker() {
        setErrorHandler(new SystemOutHandler());
        setOutputHandler(new SystemOutHandler());
        setInputStream(null);
    }

    @Override
    public final Invoker setErrorHandler(InvocationOutputHandler errorHandler) {
        this.errorHandlerCopy = errorHandler;
        return super.setErrorHandler(errorHandler);
    }

    @Override
    public final Invoker setInputStream(InputStream inputStream) {
        this.inputStreamCopy = inputStream;
        return super.setInputStream(inputStream);
    }

    @Override
    public final Invoker setOutputHandler(InvocationOutputHandler outputHandler) {
        this.outputHandlerCopy = outputHandler;
        return super.setOutputHandler(outputHandler);
    }

    private static class MyCommandLineBuilder extends org.apache.maven.shared.invoker.MavenCommandLineBuilder {
        @Override
        public Commandline build(InvocationRequest request) throws CommandLineConfigurationException {
            return super.build(request);
        }
    }

    @Override
    public InvocationResult execute( InvocationRequest request )
            throws MavenInvocationException
    {
        MavenCommandLineBuilder cliBuilder = new MyCommandLineBuilder();

        InvokerLogger logger = getLogger();
        if ( logger != null )
        {
            cliBuilder.setLogger( getLogger() );
        }

        File localRepo = getLocalRepositoryDirectory();
        if ( localRepo != null )
        {
            cliBuilder.setLocalRepositoryDirectory( getLocalRepositoryDirectory() );
        }

        File mavenHome = getMavenHome();
        if ( mavenHome != null )
        {
            cliBuilder.setMavenHome( getMavenHome() );
        }

        File mavenExecutable = getMavenExecutable();
        if ( mavenExecutable != null )
        {
            cliBuilder.setMavenExecutable( mavenExecutable );
        }

        File workingDirectory = getWorkingDirectory();
        if ( workingDirectory != null )
        {
            cliBuilder.setWorkingDirectory( getWorkingDirectory() );
        }

        Commandline cli;
        try
        {
            cli = cliBuilder.build( request );
        }
        catch ( CommandLineConfigurationException e )
        {
            throw new MavenInvocationException( "Error configuring command-line. Reason: " + e.getMessage(), e );
        }

        MyInvocationResult result = new MyInvocationResult();

        try
        {
            int exitCode = executeCommandLine( cli, request, request.getTimeoutInSeconds() );

            result.setExitCode( exitCode );
        }
        catch ( CommandLineException e )
        {
            result.setExecutionException( e );
        }

        return result;
    }

    private int executeCommandLine( Commandline cli, InvocationRequest request, int timeoutInSeconds )
            throws CommandLineException
    {
        int result;

        InputStream inputStream = request.getInputStream(inputStreamCopy);
        InvocationOutputHandler outputHandler = request.getOutputHandler(outputHandlerCopy);
        InvocationOutputHandler errorHandler = request.getErrorHandler(errorHandlerCopy);

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Executing: " + cli );
        }

        if ( request.isBatchMode() )
        {
            if ( inputStream != null )
            {
                getLogger().info( "Executing in batch mode. The configured input stream will be ignored." );
            }

            result = CommandLineUtils.executeCommandLine( cli, outputHandler, errorHandler, timeoutInSeconds );
        }
        else
        {
            if ( inputStream == null )
            {
                getLogger().warn( "Maven will be executed in interactive mode"
                        + ", but no input stream has been configured for this MavenInvoker instance." );

                result = CommandLineUtils.executeCommandLine( cli, outputHandler, errorHandler, timeoutInSeconds );
            }
            else
            {
                result = CommandLineUtils.executeCommandLine( cli, inputStream, outputHandler, errorHandler,
                        timeoutInSeconds );
            }
        }

        return result;
    }

    /*
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements.  See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership.  The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License.  You may obtain a copy of the License at
     *
     *   http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing,
     * software distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied.  See the License for the
     * specific language governing permissions and limitations
     * under the License.
     */


    /**
     * Describes the result of a Maven invocation.
     *
     */
    public static class MyInvocationResult implements InvocationResult
    {

        /**
         * The exception that prevented to execute the command line, will be <code>null</code> if Maven could be
         * successfully started.
         */
        private CommandLineException executionException;

        /**
         * The exit code reported by the Maven invocation.
         */
        private int exitCode = Integer.MIN_VALUE;

        /**
         * Creates a new invocation result
         */
        MyInvocationResult()
        {
            // hide constructor
        }

        public int getExitCode()
        {
            return exitCode;
        }

        public CommandLineException getExecutionException()
        {
            return executionException;
        }

        /**
         * Sets the exit code reported by the Maven invocation.
         *
         * @param exitCode The exit code reported by the Maven invocation.
         */
        void setExitCode( int exitCode )
        {
            this.exitCode = exitCode;
        }

        /**
         * Sets the exception that prevented to execute the command line.
         *
         * @param executionException The exception that prevented to execute the command line, may be <code>null</code>.
         */
        void setExecutionException( CommandLineException executionException )
        {
            this.executionException = executionException;
        }

    }
}
