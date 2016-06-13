var gulp         = require('gulp');
var less         = require('gulp-less');
var autoprefixer = require('gulp-autoprefixer');
var gutil        = require('gulp-util');

gulp.task('less', function () {
  gulp
    .src('./src/less/styles.less')
    .pipe(less().on('error', gutil.log))
    .pipe(autoprefixer({
      browsers: ["> 1%", "last 2 versions", "Firefox ESR", "Opera 12.1", "BlackBerry 10", "Android 4"]
    }))
    .pipe(gulp.dest('resources/public/css'))
});

gulp.task('watch', ['less'], function() {
  gulp
    .watch("src/less/**/*.less", ['less']);
})

gulp.task('default', ['less']);
