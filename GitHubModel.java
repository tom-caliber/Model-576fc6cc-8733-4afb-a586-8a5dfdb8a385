package com.sygmoid.github;

import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubResponse;
import org.eclipse.egit.github.core.service.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GitHubModel {
    private static final boolean DEBUG = true;
    private static final int RESULT_OK = 200;
    private static final String BASE_REMOTE_URL = "https://github.com/";
    private static final String BASE_REMOTE_URL_DOWNLOAD = "https://api.github.com/";

    private final String USERNAME = "tom.caliber@gmail.com";
    private final String PASSWORD = "tomesh001";
    private final String OWNER_ID = "tom-caliber";

    private  final String ZIPBALL = "zipball";
    private    final String MASTER = "master";

    private static GitHubModel gitHubModel;

    private GitHubClient client;
    private MyRepositoriesService repoService;
    private UserService userService;
    private IssueService issueService;
    private MilestoneService milestoneService;
    private LabelService labelService;
    private CommitService commitService;
    private MarkdownService markdownService;
    private CollaboratorService colaboratorService;
    private ContentsService contentsService;
    private String gitUrl = "";


    /*
     * Private constructor as it will be a singleton class
     *
     * */
    private GitHubModel() {
    }

    /*
     *
     * getInstance , This will return the GitModel object
     *
     * */
    public static GitHubModel getGitInstance() {

        if (gitHubModel == null) {

            synchronized (GitHubModel.class) {
                if (gitHubModel == null)
                    return gitHubModel = new GitHubModel();

            }

        }
        return gitHubModel;
    }

    /*
     * Connecting to GITHUB CLIENT
     *
     * */

    public GitHubClient connect() {
        try {

            if (DEBUG)
                System.out.println("Connect calling");

            client = new GitHubClient();
            client.setCredentials(USERNAME, PASSWORD);

            repoService = new MyRepositoriesService(client);
            userService = new UserService(client);
            issueService = new IssueService(client);
            milestoneService = new MilestoneService(client);
            labelService = new LabelService(client);
            commitService = new CommitService(client);
            markdownService = new MarkdownService(client);
            colaboratorService = new CollaboratorService(client);
            contentsService = new ContentsService(client);
            return client;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Create Repository
     * creating repository in github
     *
     *
     * */

    public GItResponseModel createGitHubRepo(String repo_name) {

        GItResponseModel responseModel = createRepo(repo_name);
        if (responseModel.getCode() == RESULT_OK) {
            try {
                initialCommit(responseModel.getCloneURL());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        return responseModel;

    }

    /*
     * Create repo
     * */
    private GItResponseModel createRepo(String repo_name) {

        GItResponseModel responseModel = new GItResponseModel();


        try {
            if (DEBUG)
                System.out.println("Client user is required" + client.getUser().toString());
            RepositoryService service = new RepositoryService(client);
            Repository repository = new Repository();
            repository.setOwner(new User().setLogin(client.getUser()));
            repository.setName(repo_name);
            repository.setPrivate(false);
            Repository created = service.createRepository(repository);
            if (DEBUG) {
                System.out.println(created);
                System.out.println(repository + "  ==  =" + created);
                System.out.println(created.isPrivate());
                System.out.println(repository.getOwner() + " === " + created.getOwner());
                System.out.println(repository.getName() + " === " + created.getName());
            }
            responseModel.setCode(200);
            responseModel.setGitURL(created.getGitUrl());
            responseModel.setCloneURL(created.getCloneUrl());
            responseModel.setRepoName(created.getName());
            responseModel.setUserName(created.getOwner().getName());
            return responseModel;

        } catch (IOException e) {
            e.printStackTrace();
            responseModel.setCode(400);
            return responseModel;
        }

    }


    /*
     *
     * create branch required repo name and branch name
     *
     * */
    public boolean createBranch(String repository_name, String branch_name) {
        System.out.println("Calling Create Branch");

        Git branchGit = getGit(BASE_REMOTE_URL + OWNER_ID + repository_name + ".git");
        org.eclipse.jgit.lib.Repository repository = branchGit.getRepository();
        //

        try (Git git_1 = new Git(repository)) {
            List<Ref> call = git_1.branchList().call();
            for (Ref ref : call) {
                System.out.println("Branch-Before: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            }


            // make sure the branch is not there
            List<Ref> refs = git_1.branchList().call();
            for (Ref ref : refs) {
                System.out.println("Had branch: " + ref.getName());
                if (ref.getName().equals("refs/heads/" + branch_name)) {
                    System.out.println("Removing branch before");
                    git_1.branchDelete()
                        .setBranchNames(branch_name)
                        .setForce(true)
                        .call();

                    break;
                }
            }

            // run the add-call
            git_1.branchCreate()
                .setName(branch_name)
                .call();

            call = git_1.branchList().call();
            for (Ref ref : call) {
                System.out.println("Branch-Created: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            }
            git_1.checkout();
            PushCommand pushCommand = git_1.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD));
            pushCommand.setRemote("origin");
            pushCommand.setRefSpecs(new RefSpec(branch_name));
            pushCommand.call();

            return true;
        } catch (NotMergedException e) {
            e.printStackTrace();
        } catch (InvalidRefNameException e) {
            e.printStackTrace();
        } catch (CannotDeleteCurrentBranchException e) {
            e.printStackTrace();
        } catch (RefAlreadyExistsException e) {
            e.printStackTrace();
        } catch (RefNotFoundException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return false;
    }


    /*
     * initial commit while creating repo so we can create branch later and master branch can be created by default
     *
     * */

    public void initialCommit(String gitUrl) throws IOException, GitAPIException {
        // prepare a new folder for the cloned repository
        System.out.println("");
        String fileContent = "";
        String commit_msg = "Initial commit";
        String file_name = "README";
        File localPath = getLocalPath();

        // then clone
        if (DEBUG)
            System.out.println("Cloning from " + gitUrl + " to " + localPath);
        try (Git initial_commit_git = getGit(gitUrl)) {

            gitCommit(initial_commit_git, file_name, commit_msg, fileContent);
            RevCommit result = gitPush(initial_commit_git, commit_msg);

            if (DEBUG)
                System.out.println("Pushed with commit: " + result);
        }

    }

    /*
     *
     * upload file to git
     * */
    public void uploadFilesToGit(String repo_name, List<MultipartFile> files, String commit_msg, String branch_name) {

        if (files.isEmpty()) {
            return;
        }
        String giturl = BASE_REMOTE_URL + OWNER_ID + "/" + repo_name + ".git";

        for (MultipartFile file : files) {

            try {
                String content = new String(file.getBytes(), "UTF-8");

                gitCommit(getGit(giturl, branch_name), file.getName(), commit_msg, content);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            gitPush(getGit(giturl, branch_name), commit_msg);
        }


    }


    private void gitCommit(Git initial_commit_git, String file_name, String commit_msg, String fileContent) throws IOException, GitAPIException {

        if (DEBUG)
            System.out.println(">>>>>>gitCommit<<<<<<<<");
        // create the file
        File intial_commit_file = new File(file_name);
        intial_commit_file.createNewFile();

        // Stage all files in the repo including new files
        initial_commit_git.add().addFilepattern(".").call();

        // and then commit the changes.
        initial_commit_git.commit().setMessage(commit_msg).call();

        try (PrintWriter writer = new PrintWriter(intial_commit_file)) {
            writer.append(fileContent);
        }
        // Stage all changed files, omitting new files, and commit with one command
        initial_commit_git.commit()
            .setAll(true)
            .setMessage(commit_msg)
            .call();
        initial_commit_git.add().addFilepattern("*").call();

    }

    private RevCommit gitPush(Git initial_commit_git, String commit_msg) {

        if (DEBUG)
            System.out.println(">>>>>>gitPush<<<<<<<<");
        try {

            RevCommit result = initial_commit_git.commit().setMessage(commit_msg).call();
            initial_commit_git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
                .call();

            return result;
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;

    }


    /*
     *
     * getLocal path to clone the repo
     *
     * */
    private File getLocalPath() {

        File localPath = null;
        try {
            localPath = File.createTempFile("GitRepository", "");
            if (!localPath.delete()) {
                throw new IOException("Could not delete temporary file " + localPath);
            }
            return localPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    private Git getGit(String gitUrl) {


        try (Git commit_git = Git.cloneRepository()
            .setURI(gitUrl)
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
            .setDirectory(getLocalPath())
            .call()) {

            return commit_git;
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Git getGit(String gitUrl, String branch_name) {


        try (Git commit_git = Git.cloneRepository()
            .setURI(gitUrl)
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
            .setDirectory(getLocalPath())
            .setBranch(branch_name)
            .call()) {

            return commit_git;
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }


    private GitHubResponse createPullRequest(String repo_name, String branch_name, String title, String body) {


        GitHubResponse reponse = null;
        try {
            Map<String, String> map = new HashMap<>();
            map.put("title", title);
            map.put("body", body);
            map.put("head", branch_name);
            map.put("base", "master");
            reponse = client.post("/repos/" + OWNER_ID + "/" + repo_name + "/pulls", map, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (reponse != null) {
            System.out.println(reponse.getBody().toString());
            return reponse;
        }

        return null;
    }


    public String getDownloadLink(String repo_name){

        String link = BASE_REMOTE_URL_DOWNLOAD+OWNER_ID+"/"+repo_name+"/"+ZIPBALL+"/"+MASTER;

        return link;

    }

}
