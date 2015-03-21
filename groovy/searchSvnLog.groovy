#!/usr/bin/env groovy
//
// searchSvnLog.groovy
//
def cli = new CliBuilder(
   usage: 'searchSvnLog.groovy -r <revision1> -p <revision2> -a <author> -s <stringInMessage>')
import org.apache.commons.cli.Option
cli.with
{
   h(longOpt: 'help', 'Usage Information', required: false)
   r(longOpt: 'revision1', 'First SVN Revision', args: 1, required: false)
   p(longOpt: 'revision2', 'Last SVN Revision', args: 1, required: false)
   a(longOpt: 'author', 'Revision Author', args: 1, required: false)
   s(longOpt: 'search', 'Search String', args: 1, required: false)
   t(longOpt: 'target', 'SVN target directory/URL', args: 1, required: true)
}
def opt = cli.parse(args)

if (!opt) return
if (opt.h) cli.usage()

Integer revision1 = opt.r ? (opt.r as int) : null
Integer revision2 = opt.p ? (opt.p as int) : null
if (revision1 != null && revision2 != null && revision1 > revision2)
{
   println "It makes no sense to search for revisions ${revision1} through ${revision2}."
   System.exit(-1)
}
String author = opt.a ? (opt.a as String) : null
String search = opt.s ? (opt.s as String) : null
String logTarget = opt.t

String command = "svn log -r ${revision1 ?: 1} ${revision2 ?: 'HEAD'} ${logTarget} --xml"
def proc = command.execute()
StringBuilder standard = new StringBuilder()
StringBuilder error = new StringBuilder()
proc.waitForProcessOutput(standard, error)
def returnedCode = proc.exitValue() 
if (returnedCode != 0)
{
   println "ERROR: Returned code ${returnedCode}"
}

def xmlLogOutput = standard.toString()
def log = new XmlSlurper().parseText(xmlLogOutput)
def logEntries = new TreeMap<Integer, LogEntry>()
log.logentry.each
{ svnLogEntry ->
   Integer logRevision = Integer.valueOf(svnLogEntry.@revision as String)
   String message = svnLogEntry.msg as String
   String entryAuthor = svnLogEntry.author as String
   if (   (!revision1 || revision1 <= logRevision)
       && (!revision2 || revision2 >= logRevision)
       && (!author    || author == entryAuthor)
       && (!search    || message.toLowerCase().contains(search.toLowerCase()))
      )
   {
      def logEntry =
         new LogEntry(logRevision, svnLogEntry.author as String,
                      svnLogEntry.date as String, message)
      logEntries.put(logRevision, logEntry)
   }
}
logEntries.each
{ logEntryRevisionId, logEntry ->
   println "${logEntryRevisionId} : ${logEntry.author}/${logEntry.date} : ${logEntry.message}"
}
