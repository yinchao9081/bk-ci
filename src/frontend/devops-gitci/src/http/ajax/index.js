import axios from 'axios'
import { errorHandler, successHandler } from './handler'

const request = axios.create({
    baseURL: AJAX_URL_PREFIX,
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true,
    xsrfCookieName: 'paas_perm_csrftoken',
    xsrfHeaderName: 'X-CSRFToken'
})

request.interceptors.request.use(config => config, error => Promise.reject(error))

request.interceptors.response.use(successHandler, errorHandler)

export default request

export function modifyRequestCommonHead (options) {
    request.defaults.headers.common = {
        ...request.defaults.headers.common,
        ...options
    }
}
