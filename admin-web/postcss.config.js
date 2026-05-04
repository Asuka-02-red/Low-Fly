export default {
  plugins: {
    'postcss-px-to-viewport': {
      viewportWidth: 1440, // 视窗的宽度，对应设计稿的宽度
      unitPrecision: 5, // 指定`px`转换为视窗单位值的小数位数
      viewportUnit: 'vw', // 指定需要转换成的视窗单位
      selectorBlackList: ['.ignore', '.hairlines', /^.el-/, /^.vh-/], // 指定不转换为视窗单位的类，保留 Element Plus 和视觉层级基础类
      minPixelValue: 1, // 小于或等于`1px`不转换为视窗单位
      mediaQuery: true, // 允许在媒体查询中转换`px`
      exclude: [/node_modules/]
    }
  }
}
