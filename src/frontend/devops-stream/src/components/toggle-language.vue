<template>
    <span
        class="toggle-language"
        @click="changeLanguage"
    >
        <icon
            class="language-icon"
            size="18"
            name="locale"
        ></icon>
        {{$t('changeLang')}}
    </span>
</template>

<script>
    import {
        defineComponent,
        getCurrentInstance
    } from '@vue/composition-api'
    import {
        setCookie
    } from '@/utils'

    export default defineComponent({
        setup () {
            const instance = getCurrentInstance()

            const changeLanguage = () => {
                let locale = 'zh-CN'
                try {
                    locale = instance.proxy.$i18n.locale === 'en-US' ? 'zh-CN' : 'en-US'
                } catch (e) {
                    // catch
                }
                instance.proxy.$i18n.locale = locale
                setCookie('stream_language', locale, location.hostname)
                location.reload()
            }

            return {
                changeLanguage
            }
        }
    })
</script>

<style lang="postcss" scoped>
    .toggle-language {
        display: flex;
        align-items: center;
        cursor: pointer;
        color: #c3cdd7;
        margin-right: 25px;
        &:hover {
            color: #fff;
        }
    }
    .language-icon {
        margin-right: 4px;
    }
</style>
