package com.redhat.ceylon.test.smoke.support;

import java.io.File;

import com.redhat.ceylon.common.log.Logger;
import com.redhat.ceylon.cmr.api.CmrRepository;
import com.redhat.ceylon.cmr.api.RepositoryManagerBuilder;

public class RepositoryManagerBuilderTester extends RepositoryManagerBuilder {
    public RepositoryManagerBuilderTester(Logger log, boolean offline, int timeout, String overrides) {
    }

    public RepositoryManagerBuilderTester(File mainRepository, Logger log, boolean offline, int timeout, String overrides) {
    }
    
    public RepositoryManagerBuilder addRepository(CmrRepository external) {
        return this;
    }    
}
