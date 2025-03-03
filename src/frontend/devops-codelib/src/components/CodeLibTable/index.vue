<template>
    <bk-table class="devops-codelib-table"
        :data="records"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageCountChange"
        v-bkloading="{ isLoading }"
    >
        <bk-table-column type="index" :label="$t('codelib.index')" align="center" width="60"></bk-table-column>
        <bk-table-column :label="$t('codelib.aliasName')" prop="aliasName"></bk-table-column>
        <bk-table-column :label="$t('codelib.address')" prop="url"></bk-table-column>
        <bk-table-column :label="$t('codelib.type')" prop="type" :formatter="typeFormatter"></bk-table-column>
        <bk-table-column :label="$t('codelib.authIdentity')">
            <template slot-scope="props">
                <span>{{ props.row.authType }}@</span><!--
                --><a class="text-link"
                    v-if="!['OAUTH'].includes(props.row.authType)"
                    :href="`/console/ticket/${projectId}/editCredential/${props.row.authIdentity}`"
                    target="_blank"
                >{{ props.row.authIdentity }}</a><!--
                --><span v-else>{{ props.row.authIdentity }}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('codelib.operation')" width="150">
            <template slot-scope="props">
                <bk-button theme="primary" text @click="editCodeLib(props.row)">{{ $t('codelib.edit') }}</bk-button>
                <bk-button theme="primary" text @click="deleteCodeLib(props.row)">{{ $t('codelib.delete') }}</bk-button>
            </template>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { getCodelibConfig } from '../../config/'
    export default {
        props: {
            switchPage: {
                type: Function,
                required: true
            },
            count: Number,
            hasCreatePermission: Boolean,
            totalPages: Number,
            page: Number,
            pageSize: Number,
            records: {
                type: Array,
                required: true,
                default: () => []
            }
        },

        data () {
            return {
                pagination: {
                    current: this.page,
                    count: this.count,
                    limit: this.pageSize
                },
                isLoading: false
            }
        },

        computed: {
            ...mapState('codelib', ['gitOAuth']),

            currentPage: {
                get () {
                    return this.page
                },
                set (page) {
                    this.switchPage(page, this.pageSize)
                }
            },

            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            page (val) {
                this.pagination.current = val
            },

            count (val) {
                this.pagination.count = val
            },

            pageSize (val) {
                this.pagination.limit = val
            }
        },

        created () {
            const { repoId, repoType } = this.$route.params
            if (repoId && repoType) {
                // 如果路径带有仓库ID，则弹出对应的编辑窗口
                this.editCodeLib({
                    type: repoType,
                    repositoryHashId: repoId
                })
            }
        },

        methods: {
            ...mapActions('codelib', [
                'toggleCodelibDialog',
                'requestDetail',
                'updateCodelib',
                'deleteRepo',
                'checkGitOAuth',
                'checkTGitOAuth'
            ]),

            typeFormatter (row, column, cellValue, index) {
                return cellValue.replace('CODE_', '')
            },

            handlePageChange (current) {
                this.pagination.current = current
                this.switchPage(current, this.pagination.limit)
            },

            handlePageCountChange (limit) {
                if (this.pagination.limit === limit) return

                this.pagination.current = 1
                this.pagination.limit = limit
                this.switchPage(1, limit)
            },

            async editCodeLib (codelib) {
                const { repositoryHashId, type, authType, svnType } = codelib
                const { credentialTypes, typeName } = getCodelibConfig(
                    type,
                    svnType,
                    authType
                )
                this.updateCodelib({
                    '@type': typeName
                })
                const CodelibDialog = {
                    repositoryHashId,
                    showCodelibDialog: true,
                    projectId: this.projectId,
                    credentialTypes,
                    authType,
                    codelib
                }
                this.toggleCodelibDialog(CodelibDialog)
            },

            deleteCodeLib ({ repositoryHashId, aliasName }) {
                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    subTitle: this.$t('codelib.deleteCodelib', [aliasName]),
                    confirmFn: () => {
                        const { projectId, currentPage, pageSize, count, totalPages } = this
                        this.isLoading = true

                        this.deleteRepo({ projectId, repositoryHashId }).then(() => {
                            this.$bkMessage({
                                message: `${this.$t('codelib.codelib')}${aliasName}${this.$t('codelib.successfullyDeleted')}`,
                                theme: 'success'
                            })

                            this.$router.push({
                                name: 'codelibHome',
                                params: {
                                    projectId: this.projectId,
                                    repoType: '',
                                    repoId: ''
                                }
                            })
                            if (count - 1 <= pageSize * (totalPages - 1)) {
                                // 删除列表项之后，如果不足页数的话直接切换到上一页
                                this.switchPage(currentPage - 1, pageSize)
                            } else {
                                this.switchPage(currentPage, pageSize)
                            }
                        }).catch((e) => {
                            if (e.code === 403) {
                                this.$showAskPermissionDialog({
                                    noPermissionList: [{
                                        actionId: this.$permissionActionMap.edit,
                                        resourceId: this.$permissionResourceMap.code,
                                        instanceId: [{
                                            id: repositoryHashId,
                                            name: aliasName
                                        }],
                                        projectId: this.projectId
                                    }]
                                })
                            } else {
                                this.$bkMessage({
                                    message: e.message,
                                    theme: 'error'
                                })
                            }
                        }).finally(() => {
                            this.isLoading = false
                        })
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
.devops-codelib-table {
    margin-top: 20px;
    min-width: 1200px;
    .devops-codelib-table-body td {
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    > footer {
        display: flex;
        height: 36px;
        align-items: center;
        margin-top: 30px;
        .codelib-count {
            margin-right: 20px;
        }
        .codelib-page-size {
            width: 62px;
            display: inline-block;
        }
        .codelib-paging {
            margin-left: auto;
        }
    }
}
</style>
